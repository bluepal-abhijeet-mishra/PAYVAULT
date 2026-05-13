package com.payvault.simulator.controller;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.model.FraudScenarioRequest;
import com.payvault.simulator.model.LoadTestRequest;
import com.payvault.simulator.model.ManualPaymentRequest;
import com.payvault.simulator.model.ScenarioCoverageResponse;
import com.payvault.simulator.model.ScenarioExecutionResponse;
import com.payvault.simulator.model.ScenarioSummary;
import com.payvault.simulator.model.SimulatorPublishResponse;
import com.payvault.simulator.model.SimulatorStatusResponse;
import com.payvault.simulator.model.SyntheticProfile;
import com.payvault.simulator.service.PaymentSimulatorService;
import com.payvault.simulator.service.ScenarioCatalogService;
import com.payvault.simulator.service.ScenarioExecutionService;
import com.payvault.simulator.service.SyntheticDataGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorCommandController {

    private final PaymentSimulatorService simulatorService;
    private final ScenarioCatalogService scenarioCatalogService;
    private final ScenarioExecutionService scenarioExecutionService;
    private final SyntheticDataGenerator syntheticDataGenerator;

    public SimulatorCommandController(PaymentSimulatorService simulatorService,
                                      ScenarioCatalogService scenarioCatalogService,
                                      ScenarioExecutionService scenarioExecutionService,
                                      SyntheticDataGenerator syntheticDataGenerator) {
        this.simulatorService = simulatorService;
        this.scenarioCatalogService = scenarioCatalogService;
        this.scenarioExecutionService = scenarioExecutionService;
        this.syntheticDataGenerator = syntheticDataGenerator;
    }

    @GetMapping("/status")
    public ResponseEntity<SimulatorStatusResponse> status() {
        return ResponseEntity.ok(simulatorService.getStatus());
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioSummary>> scenarios() {
        return ResponseEntity.ok(scenarioCatalogService.listScenarios());
    }

    @GetMapping("/scenarios/coverage")
    public ResponseEntity<ScenarioCoverageResponse> coverage() {
        return ResponseEntity.ok(scenarioCatalogService.getCoverage());
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<SyntheticProfile>> profiles() {
        return ResponseEntity.ok(syntheticDataGenerator.getSupportedProfiles());
    }

    @PostMapping("/scenarios/{scenarioId}/execute")
    public ResponseEntity<ScenarioExecutionResponse> executeScenario(@PathVariable String scenarioId) {
        return ResponseEntity.ok(scenarioExecutionService.execute(scenarioId));
    }

    @PostMapping("/payments")
    public ResponseEntity<SimulatorPublishResponse> publishPayment(@RequestBody(required = false) ManualPaymentRequest request) {
        return ResponseEntity.ok(simulatorService.publishManualPayment(request != null ? request : new ManualPaymentRequest()));
    }

    @PostMapping("/traffic/normal")
    public ResponseEntity<SimulatorPublishResponse> publishNormalTraffic(@RequestParam(defaultValue = "1") int count) {
        return ResponseEntity.ok(simulatorService.publishNormalTraffic(count));
    }

    @PostMapping("/fraud")
    public ResponseEntity<SimulatorPublishResponse> triggerFraud(@RequestBody FraudScenarioRequest request) {
        FraudScenario scenario = request.getScenario() != null ? request.getScenario() : FraudScenario.NONE;
        return ResponseEntity.ok(simulatorService.triggerFraudScenario(request.getUserId(), scenario));
    }

    @PostMapping("/load")
    public ResponseEntity<SimulatorPublishResponse> load(@RequestBody(required = false) LoadTestRequest request) {
        return ResponseEntity.ok(simulatorService.runLoadTest(request != null ? request : new LoadTestRequest()));
    }
}
