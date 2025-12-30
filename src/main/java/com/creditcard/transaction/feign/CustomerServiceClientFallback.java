package com.creditcard.transaction.feign;

import com.creditcard.transaction.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public ApiResponse<CustomerDTO> getCustomerById(Long id) {
        log.error("Customer Service is down. Fallback triggered for getCustomerById: {}", id);
        
        return new ApiResponse<>(
            false,
            "Customer Service is temporarily unavailable",
            null,
            null,
            LocalDateTime.now()
        );
    }

    @Override
    public ApiResponse<Boolean> customerExists(Long id) {
        log.error("Customer Service is down. Fallback triggered for customerExists: {}", id);
        
        return new ApiResponse<>(
            false,
            "Customer Service is temporarily unavailable",
            null,
            null,
            LocalDateTime.now()
        );
    }
}