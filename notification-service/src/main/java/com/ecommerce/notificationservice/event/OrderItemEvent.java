package com.ecommerce.notificationservice.event;

public record OrderItemEvent(String skuCode, Integer quantity) {
}
