package com.creditcard.transaction.controller;

import com.creditcard.transaction.dto.ApiResponse;
import com.creditcard.transaction.dto.TransactionRequestDTO;
import com.creditcard.transaction.dto.TransactionResponseDTO;
import com.creditcard.transaction.enums.TransactionStatus;
import com.creditcard.transaction.enums.TransactionType;
import com.creditcard.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
//demo
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing card transactions")
//@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
        summary = "Create a transaction",
        description = "Creates a new transaction (purchase, refund, payment, etc.)"
    )    

    
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> createTransaction(
            @Valid @RequestBody TransactionRequestDTO requestDTO) {
        
        log.info("REST request to create transaction for card: {}", requestDTO.getCardId());
        
        TransactionResponseDTO response = transactionService.createTransaction(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction created successfully"));
    }
    
    

    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves a transaction by its ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransactionById(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        
        log.info("REST request to get transaction: {}", id);
        
        TransactionResponseDTO response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction retrieved successfully"));
    }
//.
    @Operation(
        summary = "Get transaction by reference",
        description = "Retrieves a transaction by its unique reference number"
    )
    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransactionByReference(
            @Parameter(description = "Transaction reference") @PathVariable String reference) {
        
        log.info("REST request to get transaction by reference: {}", reference);
        
        TransactionResponseDTO response = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction retrieved successfully"));
    }

    @Operation(
        summary = "Get all transactions",
        description = "Retrieves all transactions with pagination"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> getAllTransactions(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "transactionDate") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("REST request to get all transactions - page: {}, size: {}", page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("DESC") ? 
                    Sort.by(sortBy).descending() : 
                    Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TransactionResponseDTO> response = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Transactions retrieved successfully"));
    }

    @Operation(
        summary = "Get transactions by card",
        description = "Retrieves all transactions for a specific card"
    )
    @GetMapping("/card/{cardId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByCardId(
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        
        log.info("REST request to get transactions for card: {}", cardId);
        
        List<TransactionResponseDTO> response = transactionService.getTransactionsByCardId(cardId);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Get transactions by card (paginated)",
        description = "Retrieves transactions for a card with pagination"
    )
    @GetMapping("/card/{cardId}/paginated")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> getTransactionsByCardIdPaginated(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        log.info("REST request to get transactions for card: {} with pagination", cardId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<TransactionResponseDTO> response = transactionService.getTransactionsByCardId(cardId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Transactions retrieved successfully"));
    }

    @Operation(
        summary = "Get transactions by customer",
        description = "Retrieves all transactions for a specific customer"
    )
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByCustomerId(
            @Parameter(description = "Customer ID") @PathVariable Long customerId) {
        
        log.info("REST request to get transactions for customer: {}", customerId);
        
        List<TransactionResponseDTO> response = transactionService.getTransactionsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Get transactions by customer (paginated)",
        description = "Retrieves transactions for a customer with pagination"
    )
    @GetMapping("/customer/{customerId}/paginated")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> getTransactionsByCustomerIdPaginated(
            @Parameter(description = "Customer ID") @PathVariable Long customerId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        log.info("REST request to get transactions for customer: {} with pagination", customerId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<TransactionResponseDTO> response = transactionService.getTransactionsByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Transactions retrieved successfully"));
    }

    @Operation(
        summary = "Get transactions by status",
        description = "Retrieves all transactions with a specific status"
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByStatus(
            @Parameter(description = "Transaction status") @PathVariable TransactionStatus status) {
        
        log.info("REST request to get transactions by status: {}", status);
        
        List<TransactionResponseDTO> response = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Get transactions by type",
        description = "Retrieves all transactions of a specific type"
    )
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByType(
            @Parameter(description = "Transaction type") @PathVariable TransactionType type) {
        
        log.info("REST request to get transactions by type: {}", type);
        
        List<TransactionResponseDTO> response = transactionService.getTransactionsByType(type);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Get transactions by date range",
        description = "Retrieves transactions within a specific date range"
    )
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("REST request to get transactions between {} and {}", startDate, endDate);
        
        List<TransactionResponseDTO> response = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Get card transactions by date range",
        description = "Retrieves transactions for a specific card within a date range"
    )
    @GetMapping("/card/{cardId}/date-range")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByCardAndDateRange(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("REST request to get transactions for card {} between {} and {}", cardId, startDate, endDate);
        
        List<TransactionResponseDTO> response = 
            transactionService.getTransactionsByCardAndDateRange(cardId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, 
            "Transactions retrieved successfully. Total: " + response.size()));
    }

    @Operation(
        summary = "Reverse a transaction",
        description = "Reverses a successful transaction (refund)"
    )
    @PostMapping("/{id}/reverse")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> reverseTransaction(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @Parameter(description = "Reversal reason") @RequestParam(required = false) String reason) {
        
        log.info("REST request to reverse transaction: {}", id);
        
        TransactionResponseDTO response = transactionService.reverseTransaction(id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction reversed successfully"));
    }

    @Operation(
        summary = "Update transaction status",
        description = "Updates the status of a transaction"
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> updateTransactionStatus(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam TransactionStatus status) {
        
        log.info("REST request to update transaction {} status to {}", id, status);
        
        TransactionResponseDTO response = transactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction status updated successfully"));
    }

    @Operation(
        summary = "Get daily spending",
        description = "Returns total spending for today for a specific card"
    )
    @GetMapping("/card/{cardId}/daily-spending")
    public ResponseEntity<ApiResponse<Double>> getDailySpending(
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        
        log.info("REST request to get daily spending for card: {}", cardId);
        
        Double spending = transactionService.getDailySpending(cardId);
        return ResponseEntity.ok(ApiResponse.success(spending, 
            "Daily spending: $" + String.format("%.2f", spending)));
    }

    @Operation(
        summary = "Get total spending",
        description = "Returns total all-time spending for a specific card"
    )
    @GetMapping("/card/{cardId}/total-spending")
    public ResponseEntity<ApiResponse<Double>> getTotalSpending(
            @Parameter(description = "Card ID") @PathVariable Long cardId) {
        
        log.info("REST request to get total spending for card: {}", cardId);
        
        Double spending = transactionService.getTotalSpending(cardId);
        return ResponseEntity.ok(ApiResponse.success(spending, 
            "Total spending: $" + String.format("%.2f", spending)));
    }
    
    @Operation(
        summary = "Count transactions by status",
        description = "Returns count of transactions for a card by status"
    )
    @GetMapping("/card/{cardId}/count")
    public ResponseEntity<ApiResponse<Long>> countTransactionsByCardAndStatus(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @Parameter(description = "Transaction status") @RequestParam TransactionStatus status) {
        
        log.info("REST request to count transactions for card {} with status {}", cardId, status);
        
        long count = transactionService.countTransactionsByCardAndStatus(cardId, status);
        return ResponseEntity.ok(ApiResponse.success(count, 
            "Transaction count: " + count));
    }
        
}