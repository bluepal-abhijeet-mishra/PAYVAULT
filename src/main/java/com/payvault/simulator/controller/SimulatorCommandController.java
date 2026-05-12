package com.payvault.simulator.controller;

import com.payvault.simulator.model.FraudScenario;
import com.payvault.simulator.service.PaymentSimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorCommandController {

    private final PaymentSimulatorService simulatorService;

    public SimulatorCommandController(PaymentSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/fraud/{userId}/{scenario}")
    public ResponseEntity<String> triggerFraud(@PathVariable String userId, @PathVariable FraudScenario scenario) {
        simulatorService.triggerFraudScenario(userId, scenario);
        return ResponseEntity.ok("Fraud scenario " + scenario + " triggered for user " + userId);
    }
}
