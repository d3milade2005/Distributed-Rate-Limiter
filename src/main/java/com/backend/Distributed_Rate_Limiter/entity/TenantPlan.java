package com.backend.Distributed_Rate_Limiter.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="tenant_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantPlan {
    @Id
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_name", nullable = false)
    private Plan plan;

    @Column(name = "fail_behavior", length = 20, nullable = false)
    private String failBehavior;
}
