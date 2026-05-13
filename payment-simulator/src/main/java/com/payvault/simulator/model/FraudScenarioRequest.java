package com.payvault.simulator.model;

import lombok.Data;

@Data
public class FraudScenarioRequest {
    private String userId;
    private FraudScenario scenario = FraudScenario.NONE;
}
