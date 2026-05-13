package com.payvault.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvault.simulator.model.ScenarioCategory;
import com.payvault.simulator.model.ScenarioCoverageResponse;
import com.payvault.simulator.model.ScenarioDefinition;
import com.payvault.simulator.model.ScenarioLibrary;
import com.payvault.simulator.model.ScenarioSummary;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScenarioCatalogService {

    private final ObjectMapper objectMapper;

    private List<ScenarioDefinition> scenarios = List.of();
    private Map<String, ScenarioDefinition> byId = Map.of();

    public ScenarioCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadScenarioLibrary() throws IOException {
        ClassPathResource resource = new ClassPathResource("scenario-library.json");
        try (InputStream stream = resource.getInputStream()) {
            ScenarioLibrary library = objectMapper.readValue(stream, ScenarioLibrary.class);
            this.scenarios = library.getScenarios();
            this.byId = scenarios.stream()
                    .collect(Collectors.toMap(scenario -> scenario.getScenarioId().toUpperCase(Locale.ROOT), Function.identity()));
        }
    }

    public List<ScenarioSummary> listScenarios() {
        return scenarios.stream()
                .map(definition -> ScenarioSummary.builder()
                        .scenarioId(definition.getScenarioId())
                        .category(definition.getCategory())
                        .title(definition.getTitle())
                        .priority(definition.getPriority())
                        .executable(definition.isExecutable())
                        .executionMode(definition.getExecutionMode())
                        .profileName(definition.getProfileName())
                        .build())
                .toList();
    }

    public ScenarioDefinition getScenario(String scenarioId) {
        return byId.get(scenarioId.toUpperCase(Locale.ROOT));
    }

    public ScenarioCoverageResponse getCoverage() {
        long executableCount = scenarios.stream().filter(ScenarioDefinition::isExecutable).count();
        List<ScenarioCategory> coveredCategories = scenarios.stream()
                .map(ScenarioDefinition::getCategory)
                .distinct()
                .toList();
        int p0Count = (int) scenarios.stream().filter(scenario -> "P0".equalsIgnoreCase(scenario.getPriority())).count();
        int p1Count = (int) scenarios.stream().filter(scenario -> "P1".equalsIgnoreCase(scenario.getPriority())).count();
        int p2Count = (int) scenarios.stream().filter(scenario -> "P2".equalsIgnoreCase(scenario.getPriority())).count();

        return ScenarioCoverageResponse.builder()
                .categoryCount(coveredCategories.size())
                .totalScenariosInPrd(scenarios.size())
                .p0ScenariosInPrd(p0Count)
                .p1ScenariosInPrd(p1Count)
                .p2ScenariosInPrd(p2Count)
                .executableScenarios((int) executableCount)
                .coveredCategories(coveredCategories)
                .build();
    }
}
