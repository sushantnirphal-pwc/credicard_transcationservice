package com.creditcard.transaction.service.impl;

import com.creditcard.transaction.dto.ApiResponse;
import com.creditcard.transaction.dto.TransactionRequestDTO;
import com.creditcard.transaction.dto.TransactionResponseDTO;
import com.creditcard.transaction.entity.Transaction;
import com.creditcard.transaction.enums.TransactionStatus;
import com.creditcard.transaction.enums.TransactionType;
import com.creditcard.transaction.exception.*;
import com.creditcard.transaction.feign.BalanceUpdateRequest;
import com.creditcard.transaction.feign.CardDTO;
import com.creditcard.transaction.feign.CardServiceClient;
import com.creditcard.transaction.feign.CustomerServiceClient;
import com.creditcard.transaction.mapper.TransactionMapper;
import com.creditcard.transaction.repository.TransactionRepository;
import com.creditcard.transaction.service.TransactionService;
import com.creditcard.transaction.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CardServiceClient cardServiceClient;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO requestDTO) {
        log.info("Creating transaction for card ID: {}", requestDTO.getCardId());

        try {
            // Validate amount
            if (!TransactionUtils.isValidAmount(requestDTO.getAmount())) {
                log.error("Invalid transaction amount: {}", requestDTO.getAmount());
                throw new InvalidTransactionException("Invalid transaction amount");
            }

            // Get card details
            log.info("Fetching card details for card ID: {}", requestDTO.getCardId());
            ApiResponse<CardDTO> cardResponse = cardServiceClient.getCardById(requestDTO.getCardId());
            
            if (!cardResponse.isSuccess() || cardResponse.getData() == null) {
                log.error("Card not found. Response: {}", cardResponse);
                throw new CardNotFoundException("Card not found with ID: " + requestDTO.getCardId());
            }

            CardDTO card = cardResponse.getData();
            log.info("Card found: ID={}, Status={}, Available Credit={}", 
                card.getId(), card.getStatus(), card.getAvailableCredit());

            // Verify card is active
            if (!"ACTIVE".equals(card.getStatus())) {
                log.error("Card is not active. Status: {}", card.getStatus());
                throw new InvalidTransactionException("Card is not active. Status: " + card.getStatus());
            }

            // Create transaction entity
            Transaction transaction = transactionMapper.toEntity(requestDTO);
            transaction.setCustomerId(card.getCustomerId());
            transaction.setTransactionReference(generateUniqueReference());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setPreviousBalance(card.getAvailableCredit());
            transaction.setCurrency("USD");

            // Process based on transaction type
            log.info("Processing {} transaction for amount: {}", requestDTO.getType(), requestDTO.getAmount());
            
            switch (requestDTO.getType()) {
                case PURCHASE:
                case CASH_ADVANCE:
                case FEE:
                    processPurchaseTransaction(transaction, card, requestDTO.getAmount());
                    break;

                case REFUND:
                case PAYMENT:
                    processRefundOrPayment(transaction, card, requestDTO.getAmount());
                    break;

                case REVERSAL:
                    throw new InvalidTransactionException("Use reverseTransaction method for reversals");

                default:
                    throw new InvalidTransactionException("Unsupported transaction type: " + requestDTO.getType());
            }

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Transaction created successfully with ID: {} and reference: {}", 
                     savedTransaction.getId(), savedTransaction.getTransactionReference());

            return transactionMapper.toDTO(savedTransaction);
            
        } catch (CardNotFoundException | InvalidTransactionException | InsufficientBalanceException e) {
            log.error("Transaction failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating transaction", e);
            throw new InvalidTransactionException("Transaction processing failed: " + e.getMessage());
        }
    }

    private void processPurchaseTransaction(Transaction transaction, CardDTO card, Double amount) {
        log.info("Processing purchase. Card: {}, Amount: {}, Available: {}", 
            card.getId(), amount, card.getAvailableCredit());
        
        try {
            // Check available credit
            if (card.getAvailableCredit() < amount) {
                String error = String.format(
                    "Insufficient credit balance. Available: %.2f, Required: %.2f", 
                    card.getAvailableCredit(), amount
                );
                transaction.setStatus(TransactionStatus.DECLINED);
                transaction.setFailureReason(error);
                log.error("Transaction declined: {}", error);
                throw new InsufficientBalanceException(error);
            }

            // Check daily limit
            Double dailySpending = getDailySpending(card.getId());
            log.info("Daily spending check - Current: {}, Limit: {}, Transaction: {}", 
                dailySpending, card.getDailyLimit(), amount);
            
            if (dailySpending != null && card.getDailyLimit() != null && 
                (dailySpending + amount) > card.getDailyLimit()) {
                String error = String.format(
                    "Daily limit exceeded. Limit: %.2f, Current: %.2f, Attempted: %.2f", 
                    card.getDailyLimit(), dailySpending, amount
                );
                transaction.setStatus(TransactionStatus.DECLINED);
                transaction.setFailureReason(error);
                log.error("Transaction declined: {}", error);
                throw new InvalidTransactionException(error);
            }

            // Update card balance - Create BalanceUpdateRequest
            log.info("Calling Card Service to debit amount: {}", amount);
            BalanceUpdateRequest balanceRequest = new BalanceUpdateRequest(amount, true);
            
            ApiResponse<CardDTO> updateResponse = cardServiceClient.updateCardBalance(
                card.getId(), balanceRequest
            );

            log.info("Card Service update response: success={}, message={}", 
                updateResponse.isSuccess(), updateResponse.getMessage());

            if (!updateResponse.isSuccess() || updateResponse.getData() == null) {
                String error = "Failed to update card balance: " + updateResponse.getMessage();
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(error);
                log.error(error);
                throw new InvalidTransactionException(error);
            }

            CardDTO updatedCard = updateResponse.getData();
            transaction.setNewBalance(updatedCard.getAvailableCredit());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setAuthorizationCode(TransactionUtils.generateAuthorizationCode());
            
            log.info("Purchase successful. New balance: {}", updatedCard.getAvailableCredit());

        } catch (InsufficientBalanceException | InvalidTransactionException e) {
            throw e;
        } catch (Exception e) {
            String error = "Card Service error: " + e.getMessage();
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(error);
            log.error("Feign exception calling Card Service", e);
            throw new InvalidTransactionException(error);
        }
    }

    private void processRefundOrPayment(Transaction transaction, CardDTO card, Double amount) {
        log.info("Processing refund/payment. Card: {}, Amount: {}", card.getId(), amount);
        
        try {
            log.info("Calling Card Service to credit amount: {}", amount);
            
            // Create BalanceUpdateRequest - FIXED
            BalanceUpdateRequest balanceRequest = new BalanceUpdateRequest(amount, false);
            
            ApiResponse<CardDTO> updateResponse = cardServiceClient.updateCardBalance(
                card.getId(), balanceRequest
            );

            log.info("Card Service update response: success={}, message={}", 
                updateResponse.isSuccess(), updateResponse.getMessage());

            if (!updateResponse.isSuccess() || updateResponse.getData() == null) {
                String error = "Failed to update card balance: " + updateResponse.getMessage();
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason(error);
                log.error(error);
                throw new InvalidTransactionException(error);
            }

            CardDTO updatedCard = updateResponse.getData();
            transaction.setNewBalance(updatedCard.getAvailableCredit());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setAuthorizationCode(TransactionUtils.generateAuthorizationCode());
            
            log.info("Refund/Payment successful. New balance: {}", updatedCard.getAvailableCredit());

        } catch (InvalidTransactionException e) {
            throw e;
        } catch (Exception e) {
            String error = "Card Service error: " + e.getMessage();
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(error);
            log.error("Feign exception calling Card Service", e);
            throw new InvalidTransactionException(error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(Long id) {
        log.info("Fetching transaction with ID: {}", id);
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + id));

        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionByReference(String reference) {
        log.info("Fetching transaction with reference: {}", reference);
        
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException(
                    "Transaction not found with reference: " + reference));

        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getAllTransactions(Pageable pageable) {
        log.info("Fetching all transactions with pagination");
        
        return transactionRepository.findAll(pageable)
                .map(transactionMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByCardId(Long cardId) {
        log.info("Fetching all transactions for card ID: {}", cardId);
        
        return transactionRepository.findByCardId(cardId).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactionsByCardId(Long cardId, Pageable pageable) {
        log.info("Fetching transactions for card ID: {} with pagination", cardId);
        
        return transactionRepository.findByCardId(cardId, pageable)
                .map(transactionMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByCustomerId(Long customerId) {
        log.info("Fetching all transactions for customer ID: {}", customerId);
        
        return transactionRepository.findByCustomerId(customerId).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactionsByCustomerId(Long customerId, Pageable pageable) {
        log.info("Fetching transactions for customer ID: {} with pagination", customerId);
        
        return transactionRepository.findByCustomerId(customerId, pageable)
                .map(transactionMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByStatus(TransactionStatus status) {
        log.info("Fetching transactions with status: {}", status);
        
        return transactionRepository.findByStatus(status).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByType(TransactionType type) {
        log.info("Fetching transactions of type: {}", type);
        
        return transactionRepository.findByType(type).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions between {} and {}", startDate, endDate);
        
        return transactionRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionsByCardAndDateRange(
            Long cardId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions for card {} between {} and {}", cardId, startDate, endDate);
        
        return transactionRepository.findByCardIdAndDateRange(cardId, startDate, endDate).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponseDTO reverseTransaction(Long transactionId, String reason) {
        log.info("Reversing transaction ID: {} with reason: {}", transactionId, reason);

        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                    "Transaction not found with ID: " + transactionId));

        if (!originalTransaction.canBeReversed()) {
            throw new InvalidTransactionException(
                "Transaction cannot be reversed. Status: " + originalTransaction.getStatus() +
                ", Already reversed: " + originalTransaction.getIsReversed());
        }

        // Create reversal transaction
        Transaction reversalTransaction = new Transaction();
        reversalTransaction.setCardId(originalTransaction.getCardId());
        reversalTransaction.setCustomerId(originalTransaction.getCustomerId());
        reversalTransaction.setType(TransactionType.REVERSAL);
        reversalTransaction.setAmount(originalTransaction.getAmount());
        reversalTransaction.setDescription("Reversal of transaction: " + 
                                          originalTransaction.getTransactionReference() + 
                                          (reason != null ? ". Reason: " + reason : ""));
        reversalTransaction.setTransactionReference(generateUniqueReference());
        reversalTransaction.setTransactionDate(LocalDateTime.now());
        reversalTransaction.setStatus(TransactionStatus.SUCCESS);
        reversalTransaction.setCurrency(originalTransaction.getCurrency());

        // Update card balance - reverse the deduction
        try {
            // Determine if original was a debit (purchase) or credit (refund/payment)
            boolean originalWasDebit = (originalTransaction.getType() == TransactionType.PURCHASE ||
                                       originalTransaction.getType() == TransactionType.CASH_ADVANCE ||
                                       originalTransaction.getType() == TransactionType.FEE);

            // For reversal: if original was debit, we credit back; if original was credit, we debit back
            boolean reversalIsDebit = !originalWasDebit;

            log.info("Original transaction type: {}, was debit: {}, reversal is debit: {}", 
                originalTransaction.getType(), originalWasDebit, reversalIsDebit);

            // Create BalanceUpdateRequest - FIXED
            BalanceUpdateRequest balanceRequest = new BalanceUpdateRequest(
                originalTransaction.getAmount(), 
                reversalIsDebit
            );
            
            ApiResponse<CardDTO> updateResponse = cardServiceClient.updateCardBalance(
                originalTransaction.getCardId(), 
                balanceRequest
            );

            if (updateResponse.isSuccess() && updateResponse.getData() != null) {
                CardDTO updatedCard = updateResponse.getData();
                reversalTransaction.setPreviousBalance(originalTransaction.getNewBalance());
                reversalTransaction.setNewBalance(updatedCard.getAvailableCredit());
                reversalTransaction.setAuthorizationCode(TransactionUtils.generateAuthorizationCode());

                // Mark original transaction as reversed
                originalTransaction.setIsReversed(true);
                originalTransaction.setReversalReference(reversalTransaction.getTransactionReference());
                transactionRepository.save(originalTransaction);

                Transaction savedReversal = transactionRepository.save(reversalTransaction);
                log.info("Transaction reversed successfully. Reversal reference: {}, New balance: {}", 
                        savedReversal.getTransactionReference(), savedReversal.getNewBalance());

                return transactionMapper.toDTO(savedReversal);
            } else {
                throw new InvalidTransactionException("Failed to reverse card balance: " + 
                    updateResponse.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to reverse transaction: {}", e.getMessage(), e);
            throw new InvalidTransactionException("Reversal failed: " + e.getMessage());
        }
    }

    @Override
    public TransactionResponseDTO updateTransactionStatus(Long transactionId, TransactionStatus status) {
        log.info("Updating transaction {} status to {}", transactionId, status);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                    "Transaction not found with ID: " + transactionId));

        transaction.setStatus(status);
        Transaction updated = transactionRepository.save(transaction);

        log.info("Transaction status updated successfully");
        return transactionMapper.toDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getDailySpending(Long cardId) {
        Double spending = transactionRepository.getDailySpendingByCardId(cardId, TransactionType.PURCHASE);
        return spending != null ? spending : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalSpending(Long cardId) {
        Double spending = transactionRepository.getTotalSpendingByCardId(cardId);
        return spending != null ? spending : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTransactionsByCardAndStatus(Long cardId, TransactionStatus status) {
        return transactionRepository.countByCardIdAndStatus(cardId, status);
    }

    private String generateUniqueReference() {
        String reference;
        do {
            reference = TransactionUtils.generateTransactionReference();
        } while (transactionRepository.existsByTransactionReference(reference));
        return reference;
    }
}