package com.github.anandps.resilient.app;

import com.github.anandps.resilient.containers.ResilientContainer;
import com.github.anandps.resilient.containers.pulsar.PulsarProxyInitializer;
import org.apache.pulsar.client.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = PulsarProxyInitializer.class)
public class PulsarResiliencyTest {
    @SpringBootConfiguration
    static class TestSpringConfig {

    }

    @Value("${pulsar.broker.url}")
    private String pulsarBrokerUrl;

    @Autowired
    private ResilientContainer resilientContainer;


    @SuppressWarnings({"CatchAndPrintStackTrace", "FutureReturnValueIgnored"})
    @Test
    void shouldProduceAndConsumeMessagesWithHealthyNetwork() throws PulsarClientException, InterruptedException {

        // given
        int messageCount = 10;

        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl(pulsarBrokerUrl)
                .build();

        String topic = "my-topic";
        Consumer<String> consumer = pulsarClient.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName("subscription-name")
                .subscribe();

        Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
                .topic(topic)
                .create();

        // when
        IntStream.range(0, messageCount).forEach(i -> {
            try {
                producer.send("Hello there " + i);
            } catch (PulsarClientException e) {
                e.printStackTrace();
            }
        });
        producer.close();

        CountDownLatch latch = new CountDownLatch(messageCount);
        IntStream.range(0, messageCount).forEach(i -> {
            consumer.receiveAsync().thenAccept(message -> {
                latch.countDown();
            });
        });

        // then
        latch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount()).isEqualTo(0);
        consumer.close();
    }

    @Test
    void shouldProduceAndConsumeMessagesWithNetworkInterruptions() throws PulsarClientException, InterruptedException {

        // given
        ToxiproxyContainer.ContainerProxy pulsarContainerProxy = resilientContainer.extractWrapped(ToxiproxyContainer.ContainerProxy.class).get();
        int messageCount = 5;


        PulsarClient pulsarClient = PulsarClient.builder()
                .serviceUrl(pulsarBrokerUrl)
                .build();

        String topic = "my-topic";
        Consumer<String> consumer = pulsarClient.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName("subscription-name")
                .subscribe();

        Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
                .topic(topic)
                .create();

        // when - pulsar connection is interrupted
        pulsarContainerProxy.setConnectionCut(true);

        //then - wait for sometime & complete exceptionally
        assertThrows(CompletionException.class, () -> producer.sendAsync("Hello there... testing")
                .orTimeout(3, TimeUnit.SECONDS)
                .join());

        //when - pulsar connection is resumed
        pulsarContainerProxy.setConnectionCut(false);

        //then - produce & consume without problems
        IntStream.range(0, messageCount).forEach(i -> {
            try {
                producer.send("Hello there " + i);
            } catch (PulsarClientException e) {
                e.printStackTrace();
            }
        });
        producer.close();

        CountDownLatch latch = new CountDownLatch(messageCount);
        IntStream.range(0, messageCount).forEach(i -> {
            consumer.receiveAsync().thenAccept(message -> {
                latch.countDown();
            });
        });

        latch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(latch.getCount()).isEqualTo(0);
        consumer.close();
    }

}
