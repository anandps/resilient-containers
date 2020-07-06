package com.github.anandps.resilient.containers.postgres;

import com.github.anandps.resilient.containers.AbstractResilientContainerInitializer;
import com.github.anandps.resilient.containers.ResilientContainer;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * ProxyContainer for Postgres to help in simulating network failures while testing the
 * resilience feature of the application
 */
public class PostgresProxyInitializer extends AbstractResilientContainerInitializer {
    private static PostgresToxiProxyContainer postgresToxiProxyContainer;

    @Override
    protected synchronized ResilientContainer createResilientContainer(ConfigurableEnvironment environment) {
        if (postgresToxiProxyContainer == null) {
            postgresToxiProxyContainer = new PostgresToxiProxyContainer();
        }
        return postgresToxiProxyContainer;
    }

    @Override
    protected Map<String, Object> createApplicationProperties() {
        return Map.ofEntries(
                Map.entry("spring.datasource.url", postgresToxiProxyContainer.getJdbcUrl()),
                Map.entry("spring.datasource.username", postgresToxiProxyContainer.getDatabaseUser()),
                Map.entry("spring.datasource.password", postgresToxiProxyContainer.getDatabasePassword()),
                Map.entry("spring.r2dbc.url", postgresToxiProxyContainer.getR2dbcUrl()),
                Map.entry("spring.r2dbc.username", postgresToxiProxyContainer.getDatabaseUser()),
                Map.entry("spring.r2dbc.password", postgresToxiProxyContainer.getDatabasePassword())
        );
    }
}
