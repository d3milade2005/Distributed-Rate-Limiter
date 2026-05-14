package com.backend.Distributed_Rate_Limiter.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
