package com.github.anandps.resilient.containers.pulsar;

import com.github.anandps.resilient.containers.AbstractResilientContainerInitializer;
import com.github.anandps.resilient.containers.ResilientContainer;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * ProxyContainer for Pulsar to help in simulating network failures while testing the
 * resilience feature of the application
 */
public class PulsarProxyInitializer extends AbstractResilientContainerInitializer {
    private static PulsarToxiProxyContainer pulsarToxiProxyContainer;

    @Override
    protected synchronized ResilientContainer createResilientContainer(ConfigurableEnvironment environment) {
        if (pulsarToxiProxyContainer == null) {
            pulsarToxiProxyContainer = new PulsarToxiProxyContainer();
        }
        return pulsarToxiProxyContainer;
    }

    @Override
    protected Map<String, Object> createApplicationProperties() {
        return Map.ofEntries(
                Map.entry("pulsar.broker.url", pulsarToxiProxyContainer.getPulsarBrokerUrl()),
                Map.entry("pulsar.broker.http_service_url", pulsarToxiProxyContainer.getHttpServiceUrl()),
                Map.entry("pulsar.http.service.url", pulsarToxiProxyContainer.getHttpServiceUrl())
        );
    }
}
