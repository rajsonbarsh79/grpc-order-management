package com.portfolio.orders.infrastructure.grpc;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceEventLogger {

    private static final Logger log =
        LoggerFactory.getLogger(ResilienceEventLogger.class);

    @Bean
    RegistryEventConsumer<CircuitBreaker> circuitBreakerEventLogger() {
        return new RegistryEventConsumer<>() {


            @Override
            public void onEntryAddedEvent(
                EntryAddedEvent<CircuitBreaker> event) {

                CircuitBreaker breaker = event.getAddedEntry();

                breaker.getEventPublisher().onEvent(e ->
                    log.debug("RESILIENCE_CIRCUIT_BREAKER event={}", e)
                );

                breaker.getEventPublisher().onStateTransition(e ->
                    log.warn(
                        "RESILIENCE_CIRCUIT_BREAKER_STATE name={} transition={}",
                        breaker.getName(),
                        e.toString()
                    )
                );

                breaker.getEventPublisher().onCallNotPermitted(e ->
                    log.warn(
                        "RESILIENCE_CIRCUIT_BREAKER_REJECTED name={} event={}",
                        breaker.getName(),
                        e
                    )
                );
            }

            @Override
            public void onEntryRemovedEvent(
                EntryRemovedEvent<CircuitBreaker> event) {
                // No action required.
            }

            @Override
            public void onEntryReplacedEvent(
                EntryReplacedEvent<CircuitBreaker> event) {
                // No action required.
            }
        };
    }

    @Bean
    RegistryEventConsumer<Retry> retryEventLogger() {
        return new RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                Retry retry = event.getAddedEntry();

                retry.getEventPublisher()
                    .onEvent(e -> log.debug(
                        "RESILIENCE_RETRY event={}", e));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
                // No action required.
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
                // No action required.
            }
        };
    }

    @Bean
    RegistryEventConsumer<Bulkhead> bulkheadEventLogger() {
        return new RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Bulkhead> event) {
                Bulkhead bulkhead = event.getAddedEntry();

                bulkhead.getEventPublisher()
                    .onEvent(e -> log.debug(
                        "RESILIENCE_BULKHEAD event={}", e));
            }

            @Override
            public void onEntryRemovedEvent(
                EntryRemovedEvent<Bulkhead> event) {
                // No action required.
            }

            @Override
            public void onEntryReplacedEvent(
                EntryReplacedEvent<Bulkhead> event) {
                // No action required.
            }
        };
    }
}
