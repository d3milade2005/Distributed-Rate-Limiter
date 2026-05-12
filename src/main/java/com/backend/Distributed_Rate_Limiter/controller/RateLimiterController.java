package com.backend.Distributed_Rate_Limiter.controller;


import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.service.RateLimiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rate-limiter")
@RequiredArgsConstructor
@Slf4j
public class RateLimiterController {
    private final RateLimiterService rateLimiterService;

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> check(
            @Valid @RequestBody RateLimitRequest request) {

        log.info("Rate limit check - tenant: {}, user: {}, action: {}",
                request.getTenantId(),
                request.getUserId(),
                request.getAction());

        RateLimitResponse response = rateLimiterService.check(request);

        if (response.isAllowed()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(response);
    }
}

