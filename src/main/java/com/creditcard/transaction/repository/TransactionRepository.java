package com.creditcard.transaction.repository;

import com.creditcard.transaction.entity.Transaction;
import com.creditcard.transaction.enums.TransactionStatus;
import com.creditcard.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    List<Transaction> findByCardId(Long cardId);

    Page<Transaction> findByCardId(Long cardId, Pageable pageable);

    List<Transaction> findByCustomerId(Long customerId);

    Page<Transaction> findByCustomerId(Long customerId, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    List<Transaction> findByType(TransactionType type);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardId = :cardId AND t.type = :type")
    List<Transaction> findByCardIdAndType(@Param("cardId") Long cardId, @Param("type") TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.status = :status")
    Page<Transaction> findByCustomerIdAndStatus(
        @Param("customerId") Long customerId, 
        @Param("status") TransactionStatus status, 
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByTransactionDateBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.cardId = :cardId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByCardIdAndDateRange(
        @Param("cardId") Long cardId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t " +
           "WHERE t.cardId = :cardId " +
           "AND t.type = :type " +
           "AND t.status = com.creditcard.transaction.enums.TransactionStatus.SUCCESS " +
           "AND CAST(t.transactionDate AS date) = CURRENT_DATE")
    Double getDailySpendingByCardId(@Param("cardId") Long cardId, @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t " +
           "WHERE t.cardId = :cardId " +
           "AND t.type = com.creditcard.transaction.enums.TransactionType.PURCHASE " +
           "AND t.status = com.creditcard.transaction.enums.TransactionStatus.SUCCESS")
    Double getTotalSpendingByCardId(@Param("cardId") Long cardId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.cardId = :cardId AND t.status = :status")
    long countByCardIdAndStatus(@Param("cardId") Long cardId, @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.merchantName LIKE %:merchantName%")
    List<Transaction> findByMerchantNameContaining(@Param("merchantName") String merchantName);

    boolean existsByTransactionReference(String transactionReference);
}