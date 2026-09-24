package com.ecommerce.orderservice.exception;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

  public InsufficientStockException(List<String> skuCodes) {
    super("Insufficient stock for: " + String.join(", ", skuCodes));
  }
}
