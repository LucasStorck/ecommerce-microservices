package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.StockResponse;

import java.util.List;

public interface InventoryClient {

  /**
   * One entry per distinct skuCode; codes unknown to inventory come back with quantity 0.
   *
   * @throws com.ecommerce.orderservice.exception.InventoryUnavailableException if inventory
   *                                                                            cannot be reached in time or the circuit is open
   */
  List<StockResponse> checkStock(List<String> skuCodes);
}
