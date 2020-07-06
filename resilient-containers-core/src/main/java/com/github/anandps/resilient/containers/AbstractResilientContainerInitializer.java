package com.github.anandps.resilient.containers;

import org.springframework.context.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public abstract class AbstractResilientContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ResilientContainer resilientContainer = createResilientContainer(applicationContext.getEnvironment());
        applicationContext.getBeanFactory().registerSingleton("resilientContainer", resilientContainer);

        //add shutdown hook
        applicationContext.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
            @Override
            public void onApplicationEvent(ContextClosedEvent event) {
                resilientContainer.stop();
            }
        });

        //start containers
        resilientContainer.start().join();

        Map<String, Object> applicationPropertiesForDatabase = createApplicationProperties();
        MapPropertySource propertySource = new MapPropertySource(getClass().getName(), applicationPropertiesForDatabase);
        applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
    }

    protected abstract ResilientContainer createResilientContainer(ConfigurableEnvironment environment);

    protected abstract Map<String, Object> createApplicationProperties();
}
