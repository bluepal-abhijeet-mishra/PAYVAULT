package com.payvault.simulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacSecurityExecutor implements ScenarioExecutor {

    private final PayVaultRestClient restClient;

    @Override
    public void execute() {
        log.info("Executing Scenario RB-03: Risk Analyst denied audit log");

        // Fetch a Risk Analyst JWT
        String token = restClient.fetchJwtForRole("Risk Analyst");

        // Attempt to call the /audit/transactions endpoint
        Throwable thrown = catchThrowable(() -> {
            restClient.getAuditTransactions(token);
        });

        // Assert that the HTTP response is 403 Forbidden
        assertThat(thrown).isInstanceOf(WebClientResponseException.Forbidden.class);

        log.info("Scenario RB-03 executed successfully and passed assertions");
    }
}
