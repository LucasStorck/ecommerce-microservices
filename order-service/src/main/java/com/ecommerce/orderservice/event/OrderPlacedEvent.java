package com.ecommerce.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(String orderNumber, Instant ordered, BigDecimal total, List<OrderItemEvent> items) {
}
