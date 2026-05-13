package com.backend.Distributed_Rate_Limiter.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    @Id
    @Column(name = "plan_name", length = 50)
    private String planName;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "refill_rate", nullable = false)
    private Double refillRate;

    @Column(name = "window_ms", nullable = false)
    private Long windowMs;

    @Column(name = "algorithm", length = 20, nullable = false)
    private String algorithm;
}