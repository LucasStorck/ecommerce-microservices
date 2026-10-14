package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ReservationItem;

import java.util.List;

public interface InventoryClient {

  /**
   * Atomically takes the stock for all items, or none of it.
   *
   * @throws com.ecommerce.orderservice.exception.InsufficientStockException if any item falls short
   * @throws com.ecommerce.orderservice.exception.InventoryUnavailableException if inventory
   *         cannot be reached in time or the circuit is open
   */
  void reserve(List<ReservationItem> items);

  /**
   * Gives a reservation back. Best effort, used to compensate when the order could not be saved.
   *
   * @throws com.ecommerce.orderservice.exception.InventoryUnavailableException if inventory
   *         cannot be reached in time or the circuit is open
   */
  void release(List<ReservationItem> items);
}
