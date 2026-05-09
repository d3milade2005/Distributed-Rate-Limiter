package com.backend.Distributed_Rate_Limiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DistributedRateLimiterApplication {
	public static void main(String[] args) {
		SpringApplication.run(DistributedRateLimiterApplication.class, args);
	}
}