package com.payvault.simulator.model;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PaymentEvent {
    private UUID transactionId;
    private String userId;
    private long amount;
    private String currency;
    private String merchantId;
    private PaymentChannel paymentChannel;
    private String location;
    private String deviceId;
    private String timestamp;
}
