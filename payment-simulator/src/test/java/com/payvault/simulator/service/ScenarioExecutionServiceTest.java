package com.payvault.simulator.service;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.model.ScenarioCategory;
import com.payvault.simulator.model.ScenarioDefinition;
import com.payvault.simulator.model.ScenarioExecutionMode;
import com.payvault.simulator.model.SimulatorPublishResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenarioExecutionServiceTest {

    @Test
    void executesFraudScenarioUsingSimulatorService() {
        PaymentSimulatorService simulatorService = Mockito.mock(PaymentSimulatorService.class);
        ScenarioCatalogService catalogService = Mockito.mock(ScenarioCatalogService.class);
        RbacSecurityExecutor rbacSecurityExecutor = Mockito.mock(RbacSecurityExecutor.class);

        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setScenarioId("BC-02");
        definition.setCategory(ScenarioCategory.BOUNDARY_CONDITIONS);
        definition.setTitle("Velocity at N=5 - trigger fires");
        definition.setExecutable(true);
        definition.setExecutionMode(ScenarioExecutionMode.KAFKA_ASSERTION);
        definition.setFraudScenario(FraudScenario.VELOCITY_SPIKE);
        definition.setExpectedOutcomes(List.of("velocity burst emitted"));

        SimulatorPublishResponse publishResponse = SimulatorPublishResponse.builder()
                .message("published")
                .publishedCount(6)
                .scenario(FraudScenario.VELOCITY_SPIKE)
                .publishedAt("2026-05-13T00:00:00Z")
                .topicName("payments.raw")
                .transactionIds(List.of(UUID.randomUUID()))
                .build();

        when(catalogService.getScenario("BC-02")).thenReturn(definition);
        when(simulatorService.triggerFraudScenario(eq(null), eq(FraudScenario.VELOCITY_SPIKE))).thenReturn(publishResponse);

        ScenarioExecutionService service = new ScenarioExecutionService(simulatorService, catalogService, rbacSecurityExecutor);
        var response = service.execute("BC-02");

        assertThat(response.getStatus()).isEqualTo("EXECUTED");
        assertThat(response.getPublishResponse()).isSameAs(publishResponse);
        assertThat(response.getExpectedOutcomes()).containsExactly("velocity burst emitted");
        verify(simulatorService).triggerFraudScenario(null, FraudScenario.VELOCITY_SPIKE);
        verify(rbacSecurityExecutor, never()).execute();
    }

    @Test
    void executesRbacScenarioUsingExecutor() {
        PaymentSimulatorService simulatorService = Mockito.mock(PaymentSimulatorService.class);
        ScenarioCatalogService catalogService = Mockito.mock(ScenarioCatalogService.class);
        RbacSecurityExecutor rbacSecurityExecutor = Mockito.mock(RbacSecurityExecutor.class);

        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setScenarioId("RB-03");
        definition.setCategory(ScenarioCategory.RBAC_SECURITY);
        definition.setTitle("Risk Analyst denied audit log");
        definition.setExecutable(true);
        definition.setExecutionMode(ScenarioExecutionMode.REST_ASSERTION);
        definition.setExpectedOutcomes(List.of("403 returned"));

        when(catalogService.getScenario("RB-03")).thenReturn(definition);

        ScenarioExecutionService service = new ScenarioExecutionService(simulatorService, catalogService, rbacSecurityExecutor);
        var response = service.execute("RB-03");

        assertThat(response.getStatus()).isEqualTo("EXECUTED");
        assertThat(response.getPublishResponse()).isNull();
        verify(rbacSecurityExecutor).execute();
        verify(simulatorService, never()).publishManualPayment(any());
    }
}
