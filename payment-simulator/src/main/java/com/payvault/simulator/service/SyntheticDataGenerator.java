package com.payvault.simulator.service;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.model.ManualPaymentRequest;
import com.payvault.simulator.model.PaymentChannel;
import com.payvault.simulator.model.PaymentEvent;
import com.payvault.simulator.model.SyntheticProfile;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SyntheticDataGenerator {

    public static final String BLACKLISTED_MERCHANT_ID = "MERCH_BLACKLIST_999";
    public static final String BLACKLISTED_DEVICE_ID = "DEV_BLACKLIST_999";

    private static final List<String> LOCATION_POOL = List.of(
            "Mumbai",
            "Bengaluru",
            "Delhi",
            "Singapore",
            "London",
            "Tokyo",
            "Dubai"
    );

    private final List<String> userIds = new CopyOnWriteArrayList<>();
    private final List<String> merchantIds = new CopyOnWriteArrayList<>();
    private final List<String> deviceIds = new CopyOnWriteArrayList<>();

    @PostConstruct
    void init() {
        for (int index = 0; index < 100; index++) {
            userIds.add("USER_" + UUID.randomUUID().toString().substring(0, 8));
        }
        for (int index = 0; index < 50; index++) {
            merchantIds.add("MERCH_" + UUID.randomUUID().toString().substring(0, 8));
        }
        for (int index = 0; index < 150; index++) {
            deviceIds.add("DEV_" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    public PaymentEvent generateCleanTransaction() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setProfileName(SyntheticProfile.CLEAN_USER);
        return createEvent(null, request);
    }

    public PaymentEvent createEvent(String userId, ManualPaymentRequest request) {
        String effectiveUserId = hasText(userId) ? userId : (request != null ? request.getUserId() : null);
        SyntheticProfile profile = request != null && request.getProfileName() != null
                ? request.getProfileName()
                : SyntheticProfile.CLEAN_USER;

        String resolvedUserId = hasText(effectiveUserId) ? effectiveUserId : randomUserId();
        String resolvedDeviceId = resolveDeviceId(profile, request);
        String resolvedMerchantId = resolveMerchantId(profile, request);
        String resolvedLocation = resolveLocation(profile, request);
        long resolvedAmount = resolveAmount(profile, request);

        return PaymentEvent.builder()
                .transactionId(UUID.randomUUID())
                .userId(resolvedUserId)
                .amount(resolvedAmount)
                .currency(request != null && hasText(request.getCurrency()) ? request.getCurrency() : "INR")
                .merchantId(resolvedMerchantId)
                .paymentChannel(request != null && request.getPaymentChannel() != null ? request.getPaymentChannel() : randomChannel())
                .location(resolvedLocation)
                .deviceId(resolvedDeviceId)
                .timestamp(request != null && hasText(request.getTimestampOverride()) ? request.getTimestampOverride() : Instant.now().toString())
                .build();
    }

    public List<PaymentEvent> generateScenario(String userId, FraudScenario scenario) {
        String effectiveUserId = hasText(userId) ? userId : randomUserId();

        return switch (scenario) {
            case NONE -> List.of(createEvent(effectiveUserId, null));
            case VELOCITY_SPIKE -> generateVelocitySpike(effectiveUserId);
            case HIGH_VALUE_SPIKE -> List.of(generateHighValueSpike(effectiveUserId));
            case GEO_ANOMALY -> generateGeoAnomaly(effectiveUserId);
            case BLACKLISTED_MERCHANT -> List.of(generateBlacklistedMerchantTxn(effectiveUserId));
            case BLACKLISTED_DEVICE -> List.of(generateBlacklistedDeviceTxn(effectiveUserId));
            case ODD_HOURS -> List.of(generateOddHoursTxn(effectiveUserId));
        };
    }

    public String randomUserId() {
        return userIds.get(ThreadLocalRandom.current().nextInt(userIds.size()));
    }

    public PaymentEvent generateLoadTransaction() {
        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setProfileName(SyntheticProfile.CLEAN_USER);
        PaymentEvent event = createEvent(null, request);
        event.setMerchantId("MERCH_LOAD_TEST");
        event.setTimestamp(Instant.now().toString());
        return event;
    }

    public List<SyntheticProfile> getSupportedProfiles() {
        return List.of(SyntheticProfile.values());
    }

    private List<PaymentEvent> generateVelocitySpike(String userId) {
        List<PaymentEvent> events = new ArrayList<>();
        Instant start = Instant.now();
        for (int count = 0; count < 6; count++) {
            PaymentEvent event = createEvent(userId, null);
            event.setTimestamp(start.plus(count * 5L, ChronoUnit.SECONDS).toString());
            events.add(event);
        }
        return events;
    }

    private PaymentEvent generateHighValueSpike(String userId) {
        PaymentEvent event = createEvent(userId, null);
        event.setAmount(300_000L);
        return event;
    }

    private List<PaymentEvent> generateGeoAnomaly(String userId) {
        Instant start = Instant.now();

        PaymentEvent first = createEvent(userId, null);
        first.setLocation("Mumbai");
        first.setTimestamp(start.toString());

        PaymentEvent second = createEvent(userId, null);
        second.setLocation("London");
        second.setTimestamp(start.plus(2, ChronoUnit.MINUTES).toString());

        return List.of(first, second);
    }

    private PaymentEvent generateBlacklistedMerchantTxn(String userId) {
        PaymentEvent event = createEvent(userId, null);
        event.setMerchantId(BLACKLISTED_MERCHANT_ID);
        return event;
    }

    private PaymentEvent generateBlacklistedDeviceTxn(String userId) {
        PaymentEvent event = createEvent(userId, null);
        event.setDeviceId(BLACKLISTED_DEVICE_ID);
        return event;
    }

    private PaymentEvent generateOddHoursTxn(String userId) {
        PaymentEvent event = createEvent(userId, null);
        ZonedDateTime oddHour = ZonedDateTime.now(ZoneOffset.UTC)
                .withHour(2)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        event.setTimestamp(oddHour.toInstant().toString());
        return event;
    }

    private long randomAmount() {
        return ThreadLocalRandom.current().nextLong(1_000, 50_000);
    }

    private long resolveAmount(SyntheticProfile profile, ManualPaymentRequest request) {
        if (request != null && request.getAmount() != null) {
            return request.getAmount();
        }

        return switch (profile) {
            case NEW_USER -> 15_000L;
            case HIGH_RISK_USER -> 30_000L;
            case HIGH_VALUE_SPIKE -> 300_000L;
            default -> randomAmount();
        };
    }

    private PaymentChannel randomChannel() {
        PaymentChannel[] channels = PaymentChannel.values();
        return channels[ThreadLocalRandom.current().nextInt(channels.length)];
    }

    private String randomMerchantId() {
        return merchantIds.get(ThreadLocalRandom.current().nextInt(merchantIds.size()));
    }

    private String randomDeviceId() {
        return deviceIds.get(ThreadLocalRandom.current().nextInt(deviceIds.size()));
    }

    private String randomLocation() {
        return LOCATION_POOL.get(ThreadLocalRandom.current().nextInt(LOCATION_POOL.size()));
    }

    private String resolveMerchantId(SyntheticProfile profile, ManualPaymentRequest request) {
        if (request != null && hasText(request.getMerchantId())) {
            return request.getMerchantId();
        }

        return switch (profile) {
            case BLACKLISTED_MERCHANT -> BLACKLISTED_MERCHANT_ID;
            default -> randomMerchantId();
        };
    }

    private String resolveDeviceId(SyntheticProfile profile, ManualPaymentRequest request) {
        if (request != null && hasText(request.getDeviceId())) {
            return request.getDeviceId();
        }

        return switch (profile) {
            case NEW_USER -> "DEV_NEW_" + UUID.randomUUID().toString().substring(0, 8);
            case BLACKLISTED_DEVICE -> BLACKLISTED_DEVICE_ID;
            default -> randomDeviceId();
        };
    }

    private String resolveLocation(SyntheticProfile profile, ManualPaymentRequest request) {
        if (request != null && hasText(request.getLocation())) {
            return request.getLocation();
        }

        return switch (profile) {
            case GEO_ANOMALY_PAIR -> "Mumbai";
            default -> randomLocation();
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
