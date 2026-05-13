package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulatorStatusResponse {
    private String topicName;
    private boolean autoTrafficEnabled;
    private long totalPublished;
    private long scenarioPublished;
    private String lastPublishedAt;
    private int configuredMinBatchSize;
    private int configuredMaxBatchSize;
    private double chaosProbability;
}
