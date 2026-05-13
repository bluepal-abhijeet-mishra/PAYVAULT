package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScenarioExecutionResponse {
    private String scenarioId;
    private ScenarioCategory category;
    private String title;
    private String status;
    private String detail;
    private List<String> expectedOutcomes;
    private SimulatorPublishResponse publishResponse;
}
