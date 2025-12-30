package com.creditcard.transaction.dto;

import com.creditcard.transaction.enums.TransactionStatus;
import com.creditcard.transaction.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long id;
    private String transactionReference;
    private Long cardId;
    private Long customerId;
    private TransactionType type;
    private Double amount;
    private String currency;
    private String description;
    private String merchantName;
    private String merchantCategory;
    private TransactionStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate;

    private Double previousBalance;
    private Double newBalance;
    private String authorizationCode;
    private String failureReason;
    private Boolean isReversed;
    private String reversalReference;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}