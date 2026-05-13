package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScenarioCoverageResponse {
    private int categoryCount;
    private int totalScenariosInPrd;
    private int p0ScenariosInPrd;
    private int p1ScenariosInPrd;
    private int p2ScenariosInPrd;
    private int executableScenarios;
    private List<ScenarioCategory> coveredCategories;
}
