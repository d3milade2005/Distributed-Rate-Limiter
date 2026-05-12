package com.backend.Distributed_Rate_Limiter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private int remaining;
    private long resetAt;
    private String reason;
}
