package com.backend.Distributed_Rate_Limiter.strategy;

import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component("token_bucket")
@RequiredArgsConstructor
@Slf4j
public class TokenBucketStrategy implements RateLimitStrategy {
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> tokenBucketScript;

    @Override
    public RateLimitResponse check(RateLimitRequest request, TenantConfig config) {

        String key = buildKey(request);
        long now = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString(); // use unique id for each request so two requests at the same ms don't collide in the sorted set

        List<Long> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(config.getWindowMs()),
                String.valueOf(config.getCapacity()),
                String.valueOf(now),
                requestId
        );

        boolean allowed = result.get(0) == 1L;
        int remaining = result.get(1).intValue();
        long resetAt = result.get(2);

        String reason = allowed ? "ok" : "rate limit exceeded";
        return new RateLimitResponse(allowed, remaining, resetAt, reason);
    }

    private String buildKey(RateLimitRequest request) {
        return request.getTenantId() + ":" +
                request.getUserId()   + ":" +
                request.getAction();
    }

}
