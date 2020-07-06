package com.github.anandps.resilient.app;


import com.github.anandps.resilient.containers.ResilientContainer;
import com.github.anandps.resilient.containers.postgres.PostgresProxyInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ToxiproxyContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = ExampleApplication.class)
@ContextConfiguration(initializers = PostgresProxyInitializer.class)
@TestPropertySource(properties = {"spring.r2dbc.schema=classpath*:*.sql", "spring.r2dbc.initialization-mode=always", "logger.level.root=DEBUG"})
public class PostgresResiliencyTest {

    @Autowired
    private ExampleRepository exampleRepository;
    @Autowired
    private ResilientContainer resilientContainer;

    @AfterEach
    public void clear() {
        exampleRepository.deleteAll().block();
    }

    @Test
    public void shouldTestWithHealthyNetwork() {
        //given
        ExampleEntity entity = new ExampleEntity();
        entity.setName("test-entity");
        exampleRepository.save(entity).block();

        assertThat(exampleRepository.count().block()).isEqualTo(1);
    }

    @Test
    public void shouldTestWithNetworkInterruptions() {
        //given
        ExampleEntity entity = new ExampleEntity();
        entity.setName("test-entity");
        exampleRepository.save(entity).block();
        ToxiproxyContainer.ContainerProxy postgresContainerProxy = resilientContainer.extractWrapped(ToxiproxyContainer.ContainerProxy.class).get();

        //when
        postgresContainerProxy.setConnectionCut(true);

        //then
        assertThrows(IllegalStateException.class, () -> exampleRepository.count().block(Duration.ofSeconds(3)))
                .getMessage().contains("Timeout on blocking read");

        //when
        postgresContainerProxy.setConnectionCut(false);
        assertThat(exampleRepository.count().block()).isEqualTo(1);
    }
}
