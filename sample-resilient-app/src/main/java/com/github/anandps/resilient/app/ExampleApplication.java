package com.github.anandps.resilient.app;

import com.github.anandps.resilient.containers.postgres.PostgresProxyInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collections;

@SpringBootApplication
@EnableTransactionManagement
public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ExampleApplication.class);
        springApplication.addInitializers(new PostgresProxyInitializer());
        springApplication.setDefaultProperties(Collections.singletonMap("spring.r2dbc.initialization-mode", "always"));
        springApplication.run(args);
    }
}
