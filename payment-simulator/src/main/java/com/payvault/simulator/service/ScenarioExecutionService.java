package com.payvault.simulator.service;

import com.payvault.simulator.model.LoadTestRequest;
import com.payvault.simulator.model.ManualPaymentRequest;
import com.payvault.simulator.model.ScenarioDefinition;
import com.payvault.simulator.model.ScenarioExecutionMode;
import com.payvault.simulator.model.ScenarioExecutionResponse;
import com.payvault.simulator.model.SimulatorPublishResponse;
import org.springframework.stereotype.Service;

@Service
public class ScenarioExecutionService {

    private final PaymentSimulatorService paymentSimulatorService;
    private final ScenarioCatalogService scenarioCatalogService;
    private final RbacSecurityExecutor rbacSecurityExecutor;

    public ScenarioExecutionService(PaymentSimulatorService paymentSimulatorService,
                                    ScenarioCatalogService scenarioCatalogService,
                                    RbacSecurityExecutor rbacSecurityExecutor) {
        this.paymentSimulatorService = paymentSimulatorService;
        this.scenarioCatalogService = scenarioCatalogService;
        this.rbacSecurityExecutor = rbacSecurityExecutor;
    }

    public ScenarioExecutionResponse execute(String scenarioId) {
        ScenarioDefinition scenario = scenarioCatalogService.getScenario(scenarioId);
        if (scenario == null) {
            return ScenarioExecutionResponse.builder()
                    .scenarioId(scenarioId)
                    .status("NOT_FOUND")
                    .detail("Scenario is not present in the simulator catalog")
                    .build();
        }

        if (!scenario.isExecutable()) {
            return buildResponse(scenario, "PLANNED", "Scenario is cataloged from the PRD but still depends on downstream integration or assertion setup", null);
        }

        if (scenario.getExecutionMode() == ScenarioExecutionMode.REST_ASSERTION && "RB-03".equals(scenario.getScenarioId())) {
            rbacSecurityExecutor.execute();
            return buildResponse(scenario, "EXECUTED", "RBAC probe executed against PayVault REST endpoints", null);
        }

        if (scenario.getExecutionMode() == ScenarioExecutionMode.LOAD) {
            LoadTestRequest request = new LoadTestRequest();
            request.setTotalTransactions(defaultInt(scenario.getTotalTransactions(), 10_000));
            request.setConcurrency(defaultInt(scenario.getConcurrency(), 32));
            return buildResponse(scenario, "EXECUTED", "Load scenario submitted", paymentSimulatorService.runLoadTest(request));
        }

        if (scenario.getFraudScenario() != null) {
            return buildResponse(scenario, "EXECUTED", "Fraud scenario stimulus emitted by simulator",
                    paymentSimulatorService.triggerFraudScenario(null, scenario.getFraudScenario()));
        }

        if (scenario.getNormalTrafficCount() != null) {
            return buildResponse(scenario, "EXECUTED", "Traffic batch emitted by simulator",
                    paymentSimulatorService.publishNormalTraffic(scenario.getNormalTrafficCount()));
        }

        ManualPaymentRequest request = new ManualPaymentRequest();
        request.setProfileName(scenario.getProfileName());
        request.setPaymentChannel(scenario.getPaymentChannel());
        request.setCurrency(scenario.getCurrency());
        request.setAmount(scenario.getAmount());
        request.setMerchantId(scenario.getMerchantId());
        request.setDeviceId(scenario.getDeviceId());
        request.setLocation(scenario.getLocation());
        request.setTimestampOverride(scenario.getTimestampOverride());
        return buildResponse(scenario, "EXECUTED", "Manual payment stimulus emitted by simulator",
                paymentSimulatorService.publishManualPayment(request));
    }

    private ScenarioExecutionResponse buildResponse(ScenarioDefinition scenario,
                                                    String status,
                                                    String detail,
                                                    SimulatorPublishResponse publishResponse) {
        return ScenarioExecutionResponse.builder()
                .scenarioId(scenario.getScenarioId())
                .category(scenario.getCategory())
                .title(scenario.getTitle())
                .status(status)
                .detail(detail)
                .expectedOutcomes(scenario.getExpectedOutcomes())
                .publishResponse(publishResponse)
                .build();
    }

    private int defaultInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
