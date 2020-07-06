package com.github.anandps.resilient.containers.pulsar;

import com.github.anandps.resilient.containers.ResilientContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PulsarToxiProxyContainer implements ResilientContainer {
    private static final Logger LOG = LoggerFactory.getLogger(PulsarToxiProxyContainer.class);
    private static final String DEFAULT_PULSAR_VERSION = "2.5.2";
    private static final String PULSAR_VERSION_SYSTEM_PROPERTY_NAME = "pulsarVersion";
    private final String pulsarVersion;
    private final PulsarContainer pulsarContainer;

    private static final Network SHARED_NETWORK = Network.newNetwork();
    private final ToxiproxyContainer toxiproxyContainer;
    private ToxiproxyContainer.ContainerProxy proxy;

    public PulsarToxiProxyContainer() {
        this(System.getProperty(PULSAR_VERSION_SYSTEM_PROPERTY_NAME, DEFAULT_PULSAR_VERSION));
    }

    protected PulsarToxiProxyContainer(String pulsarVersion) {
        this.pulsarVersion = pulsarVersion;
        this.pulsarContainer = createPulsarContainer();
        this.toxiproxyContainer = createToxiProxyContainer();
    }

    private ToxiproxyContainer createToxiProxyContainer() {
        return new ToxiproxyContainer().withNetwork(SHARED_NETWORK);
    }

    protected PulsarContainer createPulsarContainer() {
        return doCreatePulsarContainer();
    }

    @NotNull
    protected PulsarContainer doCreatePulsarContainer() {
        PulsarContainer pulsarContainer = new PulsarContainer(pulsarVersion)
                .waitingFor(Wait.forHttp("/admin/v2/namespaces/public").forStatusCode(200).forPort(PulsarContainer.BROKER_HTTP_PORT))
                .withNetwork(SHARED_NETWORK);

        String minHeapSize = System.getProperty("pulsarMinHeapSize", "512M");
        String maxHeapSize = System.getProperty("pulsarMaxHeapSize", "512M");
        String maxDirectMemorySize = System.getProperty("pulsarMaxDirectMemorySize", "512M");

        String pulsarMem = String.format("-Xms%s -Xmx%s -XX:MaxDirectMemorySize=%s", minHeapSize, maxHeapSize, maxDirectMemorySize);
        LOG.info("Setting PULSAR_MEM=\"{}\" for Pulsar container", pulsarMem);
        pulsarContainer.addEnv("PULSAR_MEM", pulsarMem);

        return pulsarContainer;
    }

    @Override
    public CompletableFuture<Void> start() {
        return Startables.deepStart(List.of(pulsarContainer, toxiproxyContainer))
                .thenCompose(__ -> {
                    this.proxy = toxiproxyContainer.getProxy(pulsarContainer, PulsarContainer.BROKER_PORT);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public void stop() {
        LOG.info("Stopping containers.");
        toxiproxyContainer.stop();
        pulsarContainer.stop();
        this.proxy = null;
    }

    protected String getPulsarBrokerUrl() {
        return "pulsar://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort();
    }

    protected String getHttpServiceUrl() {
        return "http://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort();
    }


    @Override
    public <T> Optional<T> extractWrapped(Class<T> wrappedClass) {
        if (wrappedClass.isInstance(proxy)) {
            return Optional.of(wrappedClass.cast(proxy));
        } else {
            return Optional.empty();
        }
    }
}
