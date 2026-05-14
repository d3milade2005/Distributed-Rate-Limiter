package com.backend.Distributed_Rate_Limiter.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "api")
@Getter
@Setter

// Note: @configurationProperties tells Spring to read api.keys from application.yml and map it to the keys field automatically.
public class ApiKeyConfig {
    private String keys;

    public List<String> getKeyList() {
        if (keys == null || keys.isEmpty()) return List.of();
        return Arrays.asList(keys.split(","));
    }
}