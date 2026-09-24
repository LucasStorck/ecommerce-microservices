package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.StockResponse;
import com.ecommerce.orderservice.exception.InventoryUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.ecommerce.orderservice.config.InventoryClientConfig.INVENTORY_CIRCUIT_BREAKER;

@Component
public class InventoryClientImpl implements InventoryClient {

  private static final ParameterizedTypeReference<List<StockResponse>> STOCK_LIST =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;
  private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

  public InventoryClientImpl(@LoadBalanced RestClient.Builder restClientBuilder,
                             CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                             @Value("${inventory.url}") String inventoryUrl) {
    this.restClient = restClientBuilder.baseUrl(inventoryUrl).build();
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  @Override
  public List<StockResponse> checkStock(List<String> skuCodes) {
    // Fail closed: if stock can't be verified, the order is rejected rather than accepted blindly.
    return circuitBreakerFactory.create(INVENTORY_CIRCUIT_BREAKER).run(
        () -> restClient.get()
            .uri(uri -> uri.path("/api/inventory").queryParam("skuCode", skuCodes).build())
            .retrieve()
            .body(STOCK_LIST),
        throwable -> {
          throw new InventoryUnavailableException(throwable);
        });
  }
}
