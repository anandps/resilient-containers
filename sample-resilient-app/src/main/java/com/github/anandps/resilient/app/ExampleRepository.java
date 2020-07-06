package com.github.anandps.resilient.app;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ExampleRepository extends ReactiveCrudRepository<ExampleEntity, Integer> {
}
