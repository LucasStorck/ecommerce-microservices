package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "orderNumber", ignore = true)
  @Mapping(target = "ordered", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "orderItems", source = "items")
  Order toEntity(OrderRequest request);

  @Mapping(target = "order", ignore = true)
  OrderItem toEntity(OrderItemRequest request);

  @Mapping(target = "items", source = "orderItems")
  @Mapping(target = "total", expression = "java(calculateTotal(order))")
  OrderResponse toResponse(Order order);

  OrderItemResponse toResponse(OrderItem orderItem);

  // OrderItem owns the order_id FK, so each item must point back to its order before saving.
  @AfterMapping
  default void linkItemsToOrder(@MappingTarget Order order) {
    order.getOrderItems().forEach(item -> item.setOrder(order));
  }

  default BigDecimal calculateTotal(Order order) {
    return order.getOrderItems().stream()
        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
