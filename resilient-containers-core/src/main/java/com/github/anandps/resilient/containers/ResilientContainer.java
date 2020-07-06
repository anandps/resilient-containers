package com.github.anandps.resilient.containers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ResilientContainer {

    CompletableFuture<Void> start();

    void stop();

    <T> Optional<T> extractWrapped(Class<T> wrappedClass);
}
