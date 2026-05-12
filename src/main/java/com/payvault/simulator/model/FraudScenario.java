package com.payvault.simulator.model;

public enum FraudScenario {
    NONE,
    VELOCITY_SPIKE,
    HIGH_VALUE_SPIKE,
    GEO_ANOMALY,
    BLACKLISTED_MERCHANT,
    BLACKLISTED_DEVICE,
    ODD_HOURS
}
