package com.github.anandps.resilient.app;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Configuration
@ConditionalOnProperty(name = "spring.r2dbc.initialization-mode", havingValue = "always")
@ConditionalOnBean(ConnectionFactory.class)
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
public class R2dbcCustomAutoConfigure {

    @Bean
    @ConditionalOnProperty(name = "spring.r2dbc.schema")
    public ApplicationRunner initializeDatabase(ConnectionFactory connectionFactory, @Value("${spring.r2dbc.schema}") List<String> scripts, ResourcePatternResolver resourcePatternResolver) {
        return new SchemaInitializer(resolveMigrationScriptResourcesInSortedOrder(scripts, resourcePatternResolver), connectionFactory);
    }

    static List<Resource> resolveMigrationScriptResourcesInSortedOrder(List<String> scripts, ResourcePatternResolver resourcePatternResolver) {
        return filterAndSortResources(scripts.stream().flatMap(script -> {
            try {
                return Arrays.stream(resourcePatternResolver.getResources(script));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    static List<Resource> filterAndSortResources(Stream<Resource> resourceStream) {
        return resourceStream.filter(resource -> resource.getFilename() != null)
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    private static class SchemaInitializer implements ApplicationRunner, Ordered {
        private final List<Resource> scripts;
        private final ConnectionFactory connectionFactory;

        public SchemaInitializer(List<Resource> scripts, ConnectionFactory connectionFactory) {
            this.scripts = scripts;
            this.connectionFactory = connectionFactory;
        }

        @Override
        public void run(ApplicationArguments arguments) {
            if (!scripts.isEmpty()) {
                new ResourceDatabasePopulator(scripts.toArray(new Resource[0])).execute(connectionFactory).block();
            }
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }
    }
}
