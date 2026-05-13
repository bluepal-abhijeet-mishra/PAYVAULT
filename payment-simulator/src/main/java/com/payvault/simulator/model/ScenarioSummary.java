package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenarioSummary {
    private String scenarioId;
    private ScenarioCategory category;
    private String title;
    private String priority;
    private boolean executable;
    private ScenarioExecutionMode executionMode;
    private SyntheticProfile profileName;
}
