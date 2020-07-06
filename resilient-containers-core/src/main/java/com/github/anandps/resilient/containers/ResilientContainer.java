package com.github.anandps.resilient.containers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Defines Container lifecycle
 */
public interface ResilientContainer {

    /**
     * Starts the resilient container
     *
     * @return {@link CompletableFuture<Void>}
     */
    CompletableFuture<Void> start();

    /**
     * Stops the resilient container
     */
    void stop();

    /**
     * Wrap the classes which has to be accessed in the tests
     *
     * @param wrappedClass
     * @param <T>
     * @return
     */
    <T> Optional<T> extractWrapped(Class<T> wrappedClass);
}
