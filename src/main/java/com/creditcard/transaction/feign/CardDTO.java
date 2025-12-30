package com.creditcard.transaction.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {
    private Long id;
    private String cardNumber;
    private String maskedCardNumber;
    private Long customerId;
    private String cardHolderName;
    private String cardType;
    private String status;
    private Double creditLimit;
    private Double availableCredit;
    private Double dailyLimit;
}