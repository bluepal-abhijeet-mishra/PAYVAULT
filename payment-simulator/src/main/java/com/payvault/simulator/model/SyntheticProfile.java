package com.payvault.simulator.model;

public enum SyntheticProfile {
    CLEAN_USER,
    NEW_USER,
    HIGH_RISK_USER,
    BLACKLISTED_MERCHANT,
    BLACKLISTED_DEVICE,
    GEO_ANOMALY_PAIR,
    VELOCITY_BURST,
    HIGH_VALUE_SPIKE
}
