package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.enums.Status;

public class InvalidOrderStatusException extends RuntimeException {

  public InvalidOrderStatusException(String id, Status current, Status target) {
    super("Order " + id + " is " + current + " and cannot become " + target
        + "; only PENDING orders can change status");
  }
}
