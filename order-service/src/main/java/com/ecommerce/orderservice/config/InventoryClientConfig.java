package com.ecommerce.orderservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class InventoryClientConfig {

  public static final String INVENTORY_CIRCUIT_BREAKER = "inventory";

  // @LoadBalanced makes "http://inventory-service" resolve through Eureka instead of DNS.
  // The HTTP timeouts are the real guard against a hung connection: they must be shorter
  // than the circuit breaker's time limiter below, otherwise the limiter gives up on a
  // call whose thread is still stuck waiting on the socket.
  @Bean
  @LoadBalanced
  public RestClient.Builder loadBalancedRestClientBuilder() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(1));
    requestFactory.setReadTimeout(Duration.ofSeconds(2));
    return RestClient.builder().requestFactory(requestFactory);
  }

  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> inventoryCircuitBreakerCustomizer() {
    return factory -> factory.configure(builder -> builder
        .circuitBreakerConfig(CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build())
        // Spring Cloud's default time limit is 1s, which is easy to trip by accident.
        .timeLimiterConfig(TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .build()), INVENTORY_CIRCUIT_BREAKER);
  }
}
