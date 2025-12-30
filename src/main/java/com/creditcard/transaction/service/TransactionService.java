package com.creditcard.transaction.service;

import com.creditcard.transaction.dto.TransactionRequestDTO;
import com.creditcard.transaction.dto.TransactionResponseDTO;
import com.creditcard.transaction.enums.TransactionStatus;
import com.creditcard.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionResponseDTO createTransaction(TransactionRequestDTO requestDTO);

    TransactionResponseDTO getTransactionById(Long id);

    TransactionResponseDTO getTransactionByReference(String reference);

    Page<TransactionResponseDTO> getAllTransactions(Pageable pageable);

    List<TransactionResponseDTO> getTransactionsByCardId(Long cardId);

    Page<TransactionResponseDTO> getTransactionsByCardId(Long cardId, Pageable pageable);

    List<TransactionResponseDTO> getTransactionsByCustomerId(Long customerId);

    Page<TransactionResponseDTO> getTransactionsByCustomerId(Long customerId, Pageable pageable);

    List<TransactionResponseDTO> getTransactionsByStatus(TransactionStatus status);

    List<TransactionResponseDTO> getTransactionsByType(TransactionType type);

    List<TransactionResponseDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<TransactionResponseDTO> getTransactionsByCardAndDateRange(Long cardId, LocalDateTime startDate, LocalDateTime endDate);

    TransactionResponseDTO reverseTransaction(Long transactionId, String reason);

    TransactionResponseDTO updateTransactionStatus(Long transactionId, TransactionStatus status);

    Double getDailySpending(Long cardId);

    Double getTotalSpending(Long cardId);

    long countTransactionsByCardAndStatus(Long cardId, TransactionStatus status);
}