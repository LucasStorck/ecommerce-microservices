package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(String id, String orderNumber, Instant ordered, Status status,
                            List<OrderItemResponse> items, BigDecimal total) {
}
