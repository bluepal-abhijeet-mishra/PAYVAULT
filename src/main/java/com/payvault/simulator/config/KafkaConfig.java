package com.payvault.simulator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${simulator.topic.payments-raw.name}")
    private String topicName;

    @Value("${simulator.topic.payments-raw.partitions}")
    private int partitions;

    @Value("${simulator.topic.payments-raw.replicas}")
    private int replicas;

    @Bean
    public NewTopic paymentsRawTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
