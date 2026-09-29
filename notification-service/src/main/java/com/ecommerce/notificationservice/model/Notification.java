package com.ecommerce.notificationservice.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Notification(String orderNumber, Instant ordered, BigDecimal total, int itemCount, Instant notifiedAt) {
}
