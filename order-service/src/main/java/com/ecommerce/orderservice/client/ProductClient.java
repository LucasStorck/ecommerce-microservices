package com.ecommerce.orderservice.client;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface ProductClient {

  /**
   * Looks up the current catalog price of each sku, in one call.
   *
   * @return price by skuCode, containing every requested sku
   * @throws com.ecommerce.orderservice.exception.ProductNotFoundException if any sku is not in the catalog
   * @throws com.ecommerce.orderservice.exception.ProductServiceUnavailableException if product-service
   *         cannot be reached in time or the circuit is open
   */
  Map<String, BigDecimal> getPrices(Collection<String> skuCodes);
}
