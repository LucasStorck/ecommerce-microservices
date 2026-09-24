package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(String id, String skuCode, BigDecimal price, Integer quantity) {
}
