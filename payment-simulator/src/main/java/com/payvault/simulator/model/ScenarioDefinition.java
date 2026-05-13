package com.payvault.simulator.model;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioDefinition {
    private String scenarioId;
    private ScenarioCategory category;
    private String title;
    private String priority;
    private boolean executable;
    private ScenarioExecutionMode executionMode;
    private FraudScenario fraudScenario;
    private PaymentChannel paymentChannel;
    private SyntheticProfile profileName;
    private String currency;
    private Long amount;
    private String merchantId;
    private String deviceId;
    private String location;
    private String timestampOverride;
    private Integer normalTrafficCount;
    private Integer totalTransactions;
    private Integer concurrency;
    private String detail;
    private List<String> expectedOutcomes;
}
