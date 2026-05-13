package com.payvault.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEvent {
    private UUID transactionId;
    private String decision; // e.g., "APPROVED", "DECLINED", "REVIEW"
    private List<String> ruleFlags; // e.g., ["VELOCITY_CHECK"]
    private Double riskDelta;
}
