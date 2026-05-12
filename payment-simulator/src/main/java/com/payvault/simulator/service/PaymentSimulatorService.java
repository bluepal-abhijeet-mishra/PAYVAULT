package com.payvault.simulator.service;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.model.PaymentChannel;
import com.payvault.simulator.model.PaymentEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PaymentSimulatorService {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final String topicName;
    private final double chaosProbability;

    private final List<String> userPool = new CopyOnWriteArrayList<>();
    private final List<String> merchantPool = new CopyOnWriteArrayList<>();
    private final List<String> devicePool = new CopyOnWriteArrayList<>();
    private final List<String> locationPool = List.of("New York", "San Francisco", "London", "Tokyo", "Mumbai", "Singapore", "Sydney");

    private final String BLACKLISTED_MERCHANT_ID = "MERCH_BLACKLIST_999";
    private final String BLACKLISTED_DEVICE_ID = "DEV_BLACKLIST_999";

    private final Random random = new Random();

    public PaymentSimulatorService(KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                                   @Value("${simulator.topic.payments-raw.name}") String topicName,
                                   @Value("${simulator.fraud.chaos-probability}") double chaosProbability) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.chaosProbability = chaosProbability;
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < 100; i++) {
            userPool.add("USER_" + UUID.randomUUID().toString().substring(0, 8));
        }
        for (int i = 0; i < 50; i++) {
            merchantPool.add("MERCH_" + UUID.randomUUID().toString().substring(0, 8));
        }
        for (int i = 0; i < 200; i++) {
            devicePool.add("DEV_" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    @Scheduled(fixedRateString = "${simulator.rate.normal-traffic-ms:500}")
    public void generateTraffic() {
        int batchSize = ThreadLocalRandom.current().nextInt(10, 50); // normal traffic batch
        for (int i = 0; i < batchSize; i++) {
            if (ThreadLocalRandom.current().nextDouble() < chaosProbability) {
                injectChaos();
            } else {
                generateNormalTransaction();
            }
        }
    }

    private void generateNormalTransaction() {
        PaymentEvent event = buildBaseEvent(getRandomUser());
        publishEvent(event);
    }

    private void injectChaos() {
        FraudScenario[] scenarios = FraudScenario.values();
        // pick random scenario (exclude NONE, which is 0)
        FraudScenario scenario = scenarios[ThreadLocalRandom.current().nextInt(1, scenarios.length)];
        String userId = getRandomUser();
        log.info("Injecting Chaos: {} for user {}", scenario, userId);
        triggerFraudScenario(userId, scenario);
    }

    public void triggerFraudScenario(String userId, FraudScenario scenario) {
        switch (scenario) {
            case VELOCITY_SPIKE:
                for (int i = 0; i < 6; i++) {
                    PaymentEvent event = buildBaseEvent(userId);
                    publishEvent(event);
                }
                break;
            case HIGH_VALUE_SPIKE:
                PaymentEvent highValueEvent = buildBaseEvent(userId);
                highValueEvent.setAmount(highValueEvent.getAmount() * 10); // > 5x average
                publishEvent(highValueEvent);
                break;
            case GEO_ANOMALY:
                PaymentEvent firstEvent = buildBaseEvent(userId);
                firstEvent.setLocation("Mumbai");
                publishEvent(firstEvent);

                // Simulate delay asynchronously to not block the scheduler
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(5000);
                        PaymentEvent secondEvent = buildBaseEvent(userId);
                        secondEvent.setLocation("London");
                        secondEvent.setTimestamp(Instant.now().toString());
                        publishEvent(secondEvent);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                break;
            case BLACKLISTED_MERCHANT:
                PaymentEvent bmEvent = buildBaseEvent(userId);
                bmEvent.setMerchantId(BLACKLISTED_MERCHANT_ID);
                publishEvent(bmEvent);
                break;
            case BLACKLISTED_DEVICE:
                PaymentEvent bdEvent = buildBaseEvent(userId);
                bdEvent.setDeviceId(BLACKLISTED_DEVICE_ID);
                publishEvent(bdEvent);
                break;
            case ODD_HOURS:
                PaymentEvent oddHoursEvent = buildBaseEvent(userId);
                ZonedDateTime oddTime = ZonedDateTime.now(ZoneOffset.UTC).withHour(2).withMinute(0).withSecond(0).withNano(0);
                oddHoursEvent.setTimestamp(oddTime.format(DateTimeFormatter.ISO_INSTANT));
                publishEvent(oddHoursEvent);
                break;
            default:
                break;
        }
    }

    private PaymentEvent buildBaseEvent(String userId) {
        long amount = ThreadLocalRandom.current().nextLong(1000, 50000); // 10.00 to 500.00 in lowest unit
        PaymentChannel channel = PaymentChannel.values()[ThreadLocalRandom.current().nextInt(PaymentChannel.values().length)];

        return PaymentEvent.builder()
                .transactionId(UUID.randomUUID())
                .userId(userId)
                .amount(amount)
                .currency("INR")
                .merchantId(merchantPool.get(ThreadLocalRandom.current().nextInt(merchantPool.size())))
                .paymentChannel(channel)
                .location(locationPool.get(ThreadLocalRandom.current().nextInt(locationPool.size())))
                .deviceId(devicePool.get(ThreadLocalRandom.current().nextInt(devicePool.size())))
                .timestamp(Instant.now().toString())
                .build();
    }

    private String getRandomUser() {
        return userPool.get(ThreadLocalRandom.current().nextInt(userPool.size()));
    }

    private void publishEvent(PaymentEvent event) {
        kafkaTemplate.send(topicName, event.getUserId(), event);
        log.debug("Published event: {}", event);
    }
}
