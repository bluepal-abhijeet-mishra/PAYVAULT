package com.payvault.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvault.simulator.model.ScenarioCategory;
import com.payvault.simulator.model.ScenarioSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioCatalogServiceTest {

    @Test
    void loadsScenarioLibraryFromResource() throws Exception {
        ScenarioCatalogService service = new ScenarioCatalogService(new ObjectMapper());

        service.loadScenarioLibrary();
        List<ScenarioSummary> scenarios = service.listScenarios();

        assertThat(scenarios).hasSize(106);
        assertThat(scenarios).extracting(ScenarioSummary::getScenarioId).contains("HP-01", "NF-21", "RB-18", "LP-10", "E2E-10");
        assertThat(service.getCoverage().getCoveredCategories())
                .contains(
                        ScenarioCategory.HAPPY_PATH,
                        ScenarioCategory.BOUNDARY_CONDITIONS,
                        ScenarioCategory.NEGATIVE_FAILURE,
                        ScenarioCategory.ALERT_ENGINE,
                        ScenarioCategory.RBAC_SECURITY,
                        ScenarioCategory.DASHBOARD_ANALYTICS,
                        ScenarioCategory.LOAD_PERFORMANCE,
                        ScenarioCategory.END_TO_END
                );
        assertThat(service.getCoverage().getTotalScenariosInPrd()).isEqualTo(106);
    }
}
