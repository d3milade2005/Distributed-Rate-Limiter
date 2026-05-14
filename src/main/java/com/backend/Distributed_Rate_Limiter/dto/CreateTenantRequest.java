package com.backend.Distributed_Rate_Limiter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTenantRequest {
    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "planName is required")
    private String planName;

    @NotBlank(message = "failBehavior is required")
    @Pattern(regexp = "fail_open|fail_closed", message = "failBehavior must be either fail_open or fail_closed")
    private String failBehavior;
}
