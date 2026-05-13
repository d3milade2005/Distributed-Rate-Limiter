package com.backend.Distributed_Rate_Limiter.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

//    this creates the topic "plan.upgraded" with 1 partition and 1 replica(Okay for dev)
    @Bean
    public NewTopic planUpgradedTopic() {
        return TopicBuilder
                .name("plan.upgraded")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
