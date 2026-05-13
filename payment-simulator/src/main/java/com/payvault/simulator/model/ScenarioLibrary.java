package com.payvault.simulator.model;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioLibrary {
    private List<ScenarioDefinition> scenarios;
}
