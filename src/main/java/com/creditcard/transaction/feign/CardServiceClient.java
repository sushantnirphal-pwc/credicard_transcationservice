package com.creditcard.transaction.feign;

import com.creditcard.transaction.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "card-service",
    url = "${card.service.url:http://localhost:8082}",
    fallback = CardServiceClientFallback.class
)
public interface CardServiceClient {

    @GetMapping("/api/cards/{id}")
    ApiResponse<CardDTO> getCardById(@PathVariable("id") Long id);

    @GetMapping("/api/cards/exists/{id}")
    ApiResponse<Boolean> cardExists(@PathVariable("id") Long id);

    @PostMapping(
        value = "/api/cards/{id}/update-balance",
        consumes = "application/json",
        produces = "application/json"
    )
    ApiResponse<CardDTO> updateCardBalance(
        @PathVariable("id") Long id,
        @RequestBody BalanceUpdateRequest request  // ✅ Changed from @RequestParam
    );
}