package com.ecommerce.orderservice.exception;

public class InventoryUnavailableException extends RuntimeException {

  public InventoryUnavailableException(Throwable cause) {
    super("Inventory service is unavailable, please try again later", cause);
  }
}
