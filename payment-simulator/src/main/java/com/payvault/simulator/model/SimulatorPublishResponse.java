package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SimulatorPublishResponse {
    private String message;
    private int publishedCount;
    private FraudScenario scenario;
    private String topicName;
    private String publishedAt;
    private SyntheticProfile profileName;
    private List<UUID> transactionIds;
}
