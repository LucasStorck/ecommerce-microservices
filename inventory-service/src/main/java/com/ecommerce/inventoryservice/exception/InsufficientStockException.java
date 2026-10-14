package com.ecommerce.inventoryservice.exception;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

  private final List<String> skuCodes;

  public InsufficientStockException(List<String> skuCodes) {
    super("Insufficient stock for: " + String.join(", ", skuCodes));
    this.skuCodes = skuCodes;
  }

  public List<String> getSkuCodes() {
    return skuCodes;
  }
}
