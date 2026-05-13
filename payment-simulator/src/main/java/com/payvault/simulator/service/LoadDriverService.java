package com.payvault.simulator.service;

import com.payvault.simulator.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LoadDriverService {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ExecutorService executorService;
    private final AtomicInteger sentCount = new AtomicInteger(0);

    public LoadDriverService(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        // Since Virtual Threads aren't available until Java 21 and we are on Java 17,
        // use a fixed thread pool to prevent OOM errors from thread explosion during load tests.
        this.executorService = Executors.newFixedThreadPool(100);
    }

    public void runLoadTest(int totalTransactions) {
        log.info("Starting load test for {} transactions", totalTransactions);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalTransactions; i++) {
            executorService.submit(() -> {
                long publishStartTime = System.currentTimeMillis();
                try {
                    UUID transactionId = UUID.randomUUID();
                    String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);

                    PaymentEvent event = PaymentEvent.builder()
                            .transactionId(transactionId)
                            .userId(userId)
                            .amount((long) (Math.random() * 10000))
                            .currency("USD")
                            .merchantId("merchant-load")
                            .build();

                    // Publish the event
                    kafkaTemplate.send("payments.raw", userId, event).whenComplete((result, ex) -> {
                        long publishLatency = System.currentTimeMillis() - publishStartTime;
                        if (ex != null) {
                            log.error("Failed to publish transactionId: {}. Latency: {} ms", transactionId, publishLatency, ex);
                        } else {
                            int count = sentCount.incrementAndGet();
                            if (count % 10000 == 0) {
                                log.info("Published {} transactions. Last publish latency: {} ms", count, publishLatency);
                            }
                        }
                    });
                } catch (Exception e) {
                    log.error("Error during load test publish", e);
                }
            });
        }

        log.info("All {} tasks submitted in {} ms. Waiting for completion...",
                totalTransactions, System.currentTimeMillis() - startTime);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
