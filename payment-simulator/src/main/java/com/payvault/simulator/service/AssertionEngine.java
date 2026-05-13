package com.payvault.simulator.service;

import com.payvault.simulator.model.DecisionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Service
public class AssertionEngine {

    private final ConcurrentMap<UUID, DecisionEvent> decisionStore = new ConcurrentHashMap<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "payments.decisions", groupId = "simulator-assertion-group")
    public void listenForDecisions(DecisionEvent decisionEvent) {
        log.info("Received decision for transactionId: {}", decisionEvent.getTransactionId());
        decisionStore.put(decisionEvent.getTransactionId(), decisionEvent);
    }

    public DecisionEvent awaitDecision(UUID transactionId, Duration timeout) {
        log.info("Awaiting decision for transactionId: {} with timeout: {}", transactionId, timeout);
        await().atMost(timeout).until(() -> decisionStore.containsKey(transactionId));
        return decisionStore.get(transactionId);
    }

    public void assertDecisionInPostgres(UUID transactionId, String expectedDecision) {
        log.info("Asserting decision for transactionId: {} in PostgreSQL is {}", transactionId, expectedDecision);
        String sql = "SELECT decision FROM transactions WHERE transaction_id = ?";
        String actualDecision = jdbcTemplate.queryForObject(sql, String.class, transactionId.toString());
        assertThat(actualDecision).isEqualTo(expectedDecision);
    }

    public void assertIdempotencyKeyExists(UUID transactionId) {
        log.info("Asserting idempotency key exists in Redis for transactionId: {}", transactionId);
        String key = "idempotency:txn:" + transactionId.toString();
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists).isTrue();
    }
}
