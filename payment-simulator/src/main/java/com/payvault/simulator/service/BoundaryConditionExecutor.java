package com.payvault.simulator.service;

import com.payvault.simulator.model.DecisionEvent;
import com.payvault.simulator.model.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoundaryConditionExecutor implements ScenarioExecutor {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final AssertionEngine assertionEngine;

    @Override
    public void execute() {
        log.info("Executing Scenario BC-02: Velocity exactly at 5 txns/min");

        String userId = UUID.randomUUID().toString();
        UUID lastTransactionId = null;

        // Generate 5 transactions for a single user
        for (int i = 0; i < 5; i++) {
            UUID transactionId = UUID.randomUUID();
            PaymentEvent event = PaymentEvent.builder()
                    .transactionId(transactionId)
                    .userId(userId)
                    .amount(1000)
                    .currency("USD")
                    .merchantId("merchant-123")
                    .build();

            log.info("Publishing transaction {} to payments.raw", i + 1);
            kafkaTemplate.send("payments.raw", userId, event);

            if (i == 4) {
                lastTransactionId = transactionId;
            }
        }

        // Use the AssertionEngine to await the decision for the 5th transaction
        DecisionEvent decision = assertionEngine.awaitDecision(lastTransactionId, Duration.ofSeconds(10));

        // Use AssertJ to verify rule_flags contains VELOCITY_CHECK
        assertThat(decision).isNotNull();
        assertThat(decision.getRuleFlags()).contains("VELOCITY_CHECK");
        assertThat(decision.getRiskDelta()).isNotNull().isGreaterThan(0.0);

        log.info("Scenario BC-02 executed successfully and passed assertions");
    }
}
