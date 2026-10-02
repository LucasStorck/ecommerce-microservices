package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ReservationItem;
import com.ecommerce.orderservice.dto.ReservationRequest;
import com.ecommerce.orderservice.dto.StockShortageResponse;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.InventoryUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.ecommerce.orderservice.config.InventoryClientConfig.INVENTORY_CIRCUIT_BREAKER;

@Component
public class InventoryClientImpl implements InventoryClient {

  private final RestClient restClient;
  private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

  public InventoryClientImpl(@LoadBalanced RestClient.Builder restClientBuilder,
                             CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                             @Value("${inventory.url}") String inventoryUrl) {
    this.restClient = restClientBuilder.baseUrl(inventoryUrl).build();
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  @Override
  public void reserve(List<ReservationItem> items) {
    // A 409 is a business answer, not an inventory failure: it is returned as a value so it
    // neither counts against the circuit breaker nor falls into the "unavailable" fallback.
    // Fail closed otherwise: if stock can't be reserved, the order is rejected.
    ResponseEntity<StockShortageResponse> response = circuitBreakerFactory.create(INVENTORY_CIRCUIT_BREAKER).run(
        () -> restClient.post()
            .uri("/api/inventory/reserve")
            .body(new ReservationRequest(items))
            .retrieve()
            .onStatus(status -> status == HttpStatus.CONFLICT, (request, res) -> {
            })
            .toEntity(StockShortageResponse.class),
        throwable -> {
          throw new InventoryUnavailableException(throwable);
        });

    if (response.getStatusCode() == HttpStatus.CONFLICT) {
      StockShortageResponse shortage = response.getBody();
      throw new InsufficientStockException(shortage != null ? shortage.insufficientSkus() : List.of());
    }
  }

  @Override
  public void release(List<ReservationItem> items) {
    circuitBreakerFactory.create(INVENTORY_CIRCUIT_BREAKER).run(
        () -> restClient.post()
            .uri("/api/inventory/release")
            .body(new ReservationRequest(items))
            .retrieve()
            .toBodilessEntity(),
        throwable -> {
          throw new InventoryUnavailableException(throwable);
        });
  }
}
