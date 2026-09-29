package com.ecommerce.orderservice.event;

public record OrderItemEvent(String skuCode, Integer quantity) {
}
