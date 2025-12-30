package com.creditcard.transaction.mapper;

import com.creditcard.transaction.dto.TransactionRequestDTO;
import com.creditcard.transaction.dto.TransactionResponseDTO;
import com.creditcard.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(TransactionRequestDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setCardId(dto.getCardId());
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setMerchantName(dto.getMerchantName());
        transaction.setMerchantCategory(dto.getMerchantCategory());
        transaction.setNotes(dto.getNotes());
        transaction.setIpAddress(dto.getIpAddress());
        return transaction;
    }

    public TransactionResponseDTO toDTO(Transaction transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setId(transaction.getId());
        dto.setTransactionReference(transaction.getTransactionReference());
        dto.setCardId(transaction.getCardId());
        dto.setCustomerId(transaction.getCustomerId());
        dto.setType(transaction.getType());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setDescription(transaction.getDescription());
        dto.setMerchantName(transaction.getMerchantName());
        dto.setMerchantCategory(transaction.getMerchantCategory());
        dto.setStatus(transaction.getStatus());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setPreviousBalance(transaction.getPreviousBalance());
        dto.setNewBalance(transaction.getNewBalance());
        dto.setAuthorizationCode(transaction.getAuthorizationCode());
        dto.setFailureReason(transaction.getFailureReason());
        dto.setIsReversed(transaction.getIsReversed());
        dto.setReversalReference(transaction.getReversalReference());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        return dto;
    }
}