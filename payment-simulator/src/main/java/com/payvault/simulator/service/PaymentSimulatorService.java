package com.payvault.simulator.service;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.model.LoadTestRequest;
import com.payvault.simulator.model.ManualPaymentRequest;
import com.payvault.simulator.model.PaymentEvent;
import com.payvault.simulator.model.SimulatorPublishResponse;
import com.payvault.simulator.model.SimulatorStatusResponse;
import com.payvault.simulator.model.SyntheticProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PaymentSimulatorService {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final SyntheticDataGenerator syntheticDataGenerator;
    private final LoadDriverService loadDriverService;
    private final String topicName;
    private final double chaosProbability;
    private final boolean autoTrafficEnabled;
    private final int minBatchSize;
    private final int maxBatchSize;
    private final AtomicLong totalPublished = new AtomicLong();
    private final AtomicLong scenarioPublished = new AtomicLong();
    private volatile Instant lastPublishedAt;

    public PaymentSimulatorService(KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                                   SyntheticDataGenerator syntheticDataGenerator,
                                   LoadDriverService loadDriverService,
                                   @Value("${simulator.topic.payments-raw.name}") String topicName,
                                   @Value("${simulator.fraud.chaos-probability}") double chaosProbability,
                                   @Value("${simulator.traffic.auto-enabled:true}") boolean autoTrafficEnabled,
                                   @Value("${simulator.traffic.min-batch-size:10}") int minBatchSize,
                                   @Value("${simulator.traffic.max-batch-size:50}") int maxBatchSize) {
        this.kafkaTemplate = kafkaTemplate;
        this.syntheticDataGenerator = syntheticDataGenerator;
        this.loadDriverService = loadDriverService;
        this.topicName = topicName;
        this.chaosProbability = chaosProbability;
        this.autoTrafficEnabled = autoTrafficEnabled;
        this.minBatchSize = minBatchSize;
        this.maxBatchSize = maxBatchSize;
    }

    @Scheduled(fixedRateString = "${simulator.rate.normal-traffic-ms:500}")
    public void generateTraffic() {
        if (!autoTrafficEnabled) {
            return;
        }

        int batchSize = ThreadLocalRandom.current().nextInt(minBatchSize, maxBatchSize + 1);
        for (int i = 0; i < batchSize; i++) {
            if (ThreadLocalRandom.current().nextDouble() < chaosProbability) {
                injectChaos();
            } else {
                publishEvent(syntheticDataGenerator.generateCleanTransaction(), false);
            }
        }
    }

    public SimulatorPublishResponse publishManualPayment(ManualPaymentRequest request) {
        PaymentEvent event = syntheticDataGenerator.createEvent(null, request);
        publishEvent(event, false);
        return buildResponse("Published manual payment", FraudScenario.NONE, request.getProfileName(), List.of(event));
    }

    public SimulatorPublishResponse publishNormalTraffic(int count) {
        List<PaymentEvent> events = java.util.stream.IntStream.range(0, Math.max(1, count))
                .mapToObj(index -> syntheticDataGenerator.generateCleanTransaction())
                .toList();
        publishEvents(events, false);
        return buildResponse("Published normal traffic batch", FraudScenario.NONE, SyntheticProfile.CLEAN_USER, events);
    }

    private void injectChaos() {
        FraudScenario[] scenarios = FraudScenario.values();
        FraudScenario scenario = scenarios[ThreadLocalRandom.current().nextInt(1, scenarios.length)];
        String userId = syntheticDataGenerator.randomUserId();
        log.info("Injecting Chaos: {} for user {}", scenario, userId);
        triggerFraudScenario(userId, scenario);
    }

    public SimulatorPublishResponse triggerFraudScenario(String userId, FraudScenario scenario) {
        List<PaymentEvent> events = syntheticDataGenerator.generateScenario(userId, scenario);
        publishEvents(events, scenario != FraudScenario.NONE);
        return buildResponse("Published fraud scenario " + scenario, scenario, null, events);
    }

    public SimulatorPublishResponse runLoadTest(LoadTestRequest request) {
        int totalTransactions = Math.max(1, request.getTotalTransactions());
        int concurrency = Math.max(1, request.getConcurrency());
        int published = loadDriverService.runLoadTest(totalTransactions, concurrency);
        totalPublished.addAndGet(published);
        lastPublishedAt = Instant.now();

        return SimulatorPublishResponse.builder()
                .message("Submitted load test batch")
                .publishedCount(published)
                .scenario(FraudScenario.NONE)
                .topicName(topicName)
                .publishedAt(Instant.now().toString())
                .transactionIds(List.of())
                .build();
    }

    public SimulatorStatusResponse getStatus() {
        return SimulatorStatusResponse.builder()
                .topicName(topicName)
                .autoTrafficEnabled(autoTrafficEnabled)
                .totalPublished(totalPublished.get())
                .scenarioPublished(scenarioPublished.get())
                .lastPublishedAt(lastPublishedAt != null ? lastPublishedAt.toString() : null)
                .configuredMinBatchSize(minBatchSize)
                .configuredMaxBatchSize(maxBatchSize)
                .chaosProbability(chaosProbability)
                .build();
    }

    private void publishEvents(List<PaymentEvent> events, boolean scenarioTraffic) {
        events.forEach(event -> publishEvent(event, scenarioTraffic));
    }

    private void publishEvent(PaymentEvent event, boolean scenarioTraffic) {
        kafkaTemplate.send(topicName, event.getUserId(), event);
        totalPublished.incrementAndGet();
        if (scenarioTraffic) {
            scenarioPublished.incrementAndGet();
        }
        lastPublishedAt = Instant.now();
        log.debug("Published event {}", event.getTransactionId());
    }

    private SimulatorPublishResponse buildResponse(String message,
                                                   FraudScenario scenario,
                                                   SyntheticProfile profileName,
                                                   List<PaymentEvent> events) {
        return SimulatorPublishResponse.builder()
                .message(message)
                .publishedCount(events.size())
                .scenario(scenario)
                .topicName(topicName)
                .publishedAt(Instant.now().toString())
                .profileName(profileName)
                .transactionIds(events.stream().map(PaymentEvent::getTransactionId).toList())
                .build();
    }
}
