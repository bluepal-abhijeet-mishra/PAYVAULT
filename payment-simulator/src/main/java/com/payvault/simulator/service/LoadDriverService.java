package com.payvault.simulator.service;

import com.payvault.simulator.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LoadDriverService {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final SyntheticDataGenerator syntheticDataGenerator;
    private final String topicName;

    public LoadDriverService(KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                             SyntheticDataGenerator syntheticDataGenerator,
                             @Value("${simulator.topic.payments-raw.name}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.syntheticDataGenerator = syntheticDataGenerator;
        this.topicName = topicName;
    }

    public int runLoadTest(int totalTransactions, int concurrency) {
        log.info("Starting load test for {} transactions", totalTransactions);
        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(1, concurrency));
        AtomicInteger sentCount = new AtomicInteger(0);

        for (int i = 0; i < totalTransactions; i++) {
            executorService.submit(() -> {
                try {
                    PaymentEvent event = syntheticDataGenerator.generateLoadTransaction();
                    kafkaTemplate.send(topicName, event.getUserId(), event);
                    sentCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Error during load test publish", e);
                }
            });
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return sentCount.get();
    }
}
