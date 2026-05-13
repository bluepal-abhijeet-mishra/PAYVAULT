package com.payvault.simulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Slf4j
@Service
public class PayVaultRestClient {

    private final WebClient webClient;

    public PayVaultRestClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/api/v1").build();
    }

    public String fetchJwtForRole(String role) {
        log.info("Fetching JWT for role: {}", role);
        try {
            Map response = webClient.post()
                    .uri("/auth/token")
                    .bodyValue(Map.of("role", role))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response != null ? (String) response.get("token") : null;
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch JWT for role: {}. Error: {}", role, e.getStatusCode());
            throw e;
        }
    }

    public void resolveAlert(String alertId, String jwtToken) {
        log.info("Resolving alert: {} with token", alertId);
        webClient.post()
                .uri("/alerts/{alertId}/resolve", alertId)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public Map getAnalytics(String jwtToken) {
        log.info("Fetching analytics dashboard");
        return webClient.get()
                .uri("/dashboard/analytics")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public void getAuditTransactions(String jwtToken) {
        log.info("Attempting to get audit transactions");
        webClient.get()
                .uri("/audit/transactions")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
