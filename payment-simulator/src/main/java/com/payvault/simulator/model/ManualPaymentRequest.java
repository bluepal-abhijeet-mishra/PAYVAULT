package com.payvault.simulator.model;

import lombok.Data;

import java.util.Map;

@Data
public class ManualPaymentRequest {
    private String userId;
    private Long amount;
    private String currency;
    private String merchantId;
    private PaymentChannel paymentChannel;
    private String location;
    private String deviceId;
    private SyntheticProfile profileName;
    private String timestampOverride;
    private Map<String, String> metadata;
}
