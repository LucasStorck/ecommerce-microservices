package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {
  OrderResponse placeOrder(OrderRequest orderRequest);
  List<OrderResponse> getAllOrders();
  OrderResponse getOrderById(String id);
}
