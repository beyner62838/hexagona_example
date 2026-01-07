package com.example.PruebaTecnica.drivenadapters.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic franchiseEventsTopic() {
        return TopicBuilder.name("franchise.events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic branchEventsTopic() {
        return TopicBuilder.name("branch.events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name("product.events").partitions(1).replicas(1).build();
    }
}
