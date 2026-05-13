package com.payvault.simulator.model;

import lombok.Data;

@Data
public class LoadTestRequest {
    private int totalTransactions = 1000;
    private int concurrency = 32;
}
