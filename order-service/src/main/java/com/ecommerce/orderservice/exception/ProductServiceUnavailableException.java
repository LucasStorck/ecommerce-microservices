package com.ecommerce.orderservice.exception;

public class ProductServiceUnavailableException extends RuntimeException {

  public ProductServiceUnavailableException(Throwable cause) {
    super("Product service is unavailable, please try again later", cause);
  }
}
