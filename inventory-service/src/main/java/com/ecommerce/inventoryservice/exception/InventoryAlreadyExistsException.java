package com.ecommerce.inventoryservice.exception;

public class InventoryAlreadyExistsException extends RuntimeException {

  public InventoryAlreadyExistsException(String skuCode) {
    super("Inventory already exists for skuCode: " + skuCode);
  }
}
