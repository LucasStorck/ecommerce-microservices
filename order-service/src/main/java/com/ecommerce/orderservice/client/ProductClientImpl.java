package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductPriceResponse;
import com.ecommerce.orderservice.exception.ProductNotFoundException;
import com.ecommerce.orderservice.exception.ProductServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecommerce.orderservice.config.InventoryClientConfig.PRODUCT_CIRCUIT_BREAKER;

@Component
public class ProductClientImpl implements ProductClient {

  private final RestClient restClient;
  private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

  public ProductClientImpl(@LoadBalanced RestClient.Builder restClientBuilder,
                           CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                           @Value("${product.url}") String productUrl) {
    this.restClient = restClientBuilder.baseUrl(productUrl).build();
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  @Override
  public Map<String, BigDecimal> getPrices(Collection<String> skuCodes) {
    // Fail closed: without a trusted price the order is rejected, never priced by the client.
    List<ProductPriceResponse> products = circuitBreakerFactory.create(PRODUCT_CIRCUIT_BREAKER).run(
        () -> restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/products").queryParam("skuCode", skuCodes).build())
            .retrieve()
            .body(new ParameterizedTypeReference<List<ProductPriceResponse>>() {
            }),
        throwable -> {
          throw new ProductServiceUnavailableException(throwable);
        });

    // Checked outside the circuit breaker: a missing sku is a bad order, not a product-service failure.
    Map<String, BigDecimal> prices = (products == null ? List.<ProductPriceResponse>of() : products).stream()
        .collect(Collectors.toMap(ProductPriceResponse::skuCode, ProductPriceResponse::price, (a, b) -> a));
    List<String> missing = skuCodes.stream().filter(sku -> !prices.containsKey(sku)).toList();
    if (!missing.isEmpty()) {
      throw new ProductNotFoundException(missing);
    }
    return prices;
  }
}
