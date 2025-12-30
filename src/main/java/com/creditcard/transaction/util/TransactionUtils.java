package com.creditcard.transaction.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionUtils {

    private static final SecureRandom random = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generates a unique transaction reference
     * Format: TXN-YYYYMMDDHHMMSS-XXXXXX
     */
    public static String generateTransactionReference() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        String randomPart = generateRandomString(6);
        
        return "TXN-" + timestamp + "-" + randomPart;
    }

    /**
     * Generates authorization code
     * Format: AUTH-XXXXXX
     */
    public static String generateAuthorizationCode() {
        return "AUTH-" + generateRandomString(8);
    }

    /**
     * Generates random alphanumeric string
     */
    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Validates transaction amount
     */
    public static boolean isValidAmount(Double amount) {
        return amount != null && amount > 0 && amount <= 1000000;
    }

    /**
     * Formats amount to 2 decimal places
     */
    public static Double formatAmount(Double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}