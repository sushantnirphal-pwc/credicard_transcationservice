package com.creditcard.transaction.feign;

import com.creditcard.transaction.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "customer-service",
    url = "${customer.service.url:http://localhost:8081}",
    fallback = CustomerServiceClientFallback.class
)
public interface CustomerServiceClient {

    @GetMapping("/api/customers/{id}")
    ApiResponse<CustomerDTO> getCustomerById(@PathVariable("id") Long id);

    @GetMapping("/api/customers/exists/{id}")
    ApiResponse<Boolean> customerExists(@PathVariable("id") Long id);
}