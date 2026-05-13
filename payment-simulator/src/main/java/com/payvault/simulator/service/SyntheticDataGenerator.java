package com.payvault.simulator.service;

import com.github.javafaker.Faker;
import com.payvault.simulator.model.PaymentChannel;
import com.payvault.simulator.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class SyntheticDataGenerator {

    private final Faker faker;

    // Static mock pools to ensure recurrence of actors
    private final List<String> userIds;
    private final List<String> merchantIds;
    private final List<String> deviceIds;

    public SyntheticDataGenerator() {
        this.faker = new Faker();
        this.userIds = new ArrayList<>(50);
        this.merchantIds = new ArrayList<>(20);
        this.deviceIds = new ArrayList<>(50);

        for (int i = 0; i < 50; i++) {
            userIds.add(UUID.randomUUID().toString());
            deviceIds.add(UUID.randomUUID().toString());
        }
        for (int i = 0; i < 20; i++) {
            merchantIds.add("merch-" + UUID.randomUUID().toString().substring(0, 8));
        }
        log.info("Initialized SyntheticDataGenerator with static mock pools");
    }

    public PaymentEvent.PaymentEventBuilder baseTransactionBuilder() {
        String randomUserId = userIds.get(ThreadLocalRandom.current().nextInt(userIds.size()));
        String randomMerchantId = merchantIds.get(ThreadLocalRandom.current().nextInt(merchantIds.size()));
        String randomDeviceId = deviceIds.get(ThreadLocalRandom.current().nextInt(deviceIds.size()));

        long randomAmount = ThreadLocalRandom.current().nextLong(100, 500000);
        PaymentChannel randomChannel = PaymentChannel.values()[ThreadLocalRandom.current().nextInt(PaymentChannel.values().length)];

        return PaymentEvent.builder()
                .transactionId(UUID.randomUUID())
                .userId(randomUserId)
                .amount(randomAmount)
                .currency("USD")
                .merchantId(randomMerchantId)
                .paymentChannel(randomChannel)
                .location(faker.address().city() + ", " + faker.address().countryCode())
                .deviceId(randomDeviceId)
                .timestamp(Instant.now().toString());
    }

    public PaymentEvent generateCleanTransaction() {
        return baseTransactionBuilder().build();
    }

    public List<PaymentEvent> generateVelocityBurst(String targetUserId) {
        log.info("Generating velocity burst scenario for userId: {}", targetUserId);
        List<PaymentEvent> events = new ArrayList<>(6);
        Instant startTime = Instant.now();

        for (int i = 0; i < 6; i++) {
            Instant eventTime = startTime.plus(i * 5L, ChronoUnit.SECONDS);
            events.add(baseTransactionBuilder()
                    .userId(targetUserId)
                    .timestamp(eventTime.toString())
                    .build());
        }

        return events;
    }

    public List<PaymentEvent> generateGeoAnomaly(String targetUserId) {
        log.info("Generating geo anomaly scenario for userId: {}", targetUserId);
        List<PaymentEvent> events = new ArrayList<>(2);
        Instant startTime = Instant.now();

        events.add(baseTransactionBuilder()
                .userId(targetUserId)
                .location("Mumbai, IN")
                .timestamp(startTime.toString())
                .build());

        events.add(baseTransactionBuilder()
                .userId(targetUserId)
                .location("London, UK")
                .timestamp(startTime.plus(2, ChronoUnit.MINUTES).toString())
                .build());

        return events;
    }

    public PaymentEvent generateHighValueSpike(String targetUserId, long userAverageAmount) {
        log.info("Generating high value spike scenario for userId: {}", targetUserId);
        long spikeAmount = (userAverageAmount * 5) + 1000;

        return baseTransactionBuilder()
                .userId(targetUserId)
                .amount(spikeAmount)
                .build();
    }

    public PaymentEvent generateOddHoursTransaction() {
        log.info("Generating odd hours transaction scenario");

        // Force timestamp to 02:30 AM UTC today
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime oddHour = nowUtc.withHour(2).withMinute(30).withSecond(0).withNano(0);

        return baseTransactionBuilder()
                .timestamp(oddHour.toInstant().toString())
                .build();
    }
}
