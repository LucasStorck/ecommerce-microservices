package com.ecommerce.orderservice.exception;

import java.util.List;

public class ProductNotFoundException extends RuntimeException {

  public ProductNotFoundException(List<String> skuCodes) {
    super("Products not found in the catalog: " + String.join(", ", skuCodes));
  }
}
