package com.github.anandps.resilient.containers.postgres;

import com.github.anandps.resilient.containers.ResilientContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.*;
import org.testcontainers.lifecycle.Startables;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class PostgresToxiProxyContainer implements ResilientContainer {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresToxiProxyContainer.class);
    public static final String DEFAULT_POSTGRES_VERSION = "11-alpine";
    private String postgresVersion = System.getProperty("postgresVersion", DEFAULT_POSTGRES_VERSION);

    private static final Network SHARED_NETWORK = Network.newNetwork();
    private final ToxiproxyContainer toxiproxyContainer;
    protected final PostgreSQLContainer<?> postgresContainer;
    private ToxiproxyContainer.ContainerProxy proxy;


    public PostgresToxiProxyContainer() {
        // create all the containers using same Network
        System.out.println(">>inside toxiproxy const");
        this.postgresContainer = createPostgresContainer();
        this.toxiproxyContainer = createToxiProxyContainer();
    }


    private ToxiproxyContainer createToxiProxyContainer() {
        System.out.println(">>inside toxiproxy createToxiProxyContainer");
        return new ToxiproxyContainer().withNetwork(SHARED_NETWORK);
    }

    protected PostgreSQLContainer<?> createPostgresContainer() {
        System.out.println(">>inside postgres createPostgresContainer");
        return new PostgreSQLContainer<>("postgres:" + postgresVersion).withNetwork(SHARED_NETWORK);
    }

    @Override
    public CompletableFuture<Void> start() {
        System.out.println(">>inside toxiproxy start");
        return Startables.deepStart(List.of(postgresContainer, toxiproxyContainer))
                .thenCompose(__ -> {
                    System.out.println(">>after start::" + postgresContainer.getJdbcUrl());
                    this.proxy = toxiproxyContainer.getProxy(postgresContainer, PostgreSQLContainer.POSTGRESQL_PORT);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public void stop() {
        LOG.info("Stopping containers.");
        toxiproxyContainer.stop();
        postgresContainer.stop();
        this.proxy = null;
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort() + "/" + postgresContainer.getDatabaseName();
    }

    public String getR2dbcUrl() {
        return "r2dbc:postgresql://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort() + "/" + postgresContainer.getDatabaseName();
    }

    public String getDatabaseUser() {
        return postgresContainer.getUsername();
    }

    public String getDatabasePassword() {
        return postgresContainer.getPassword();
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
