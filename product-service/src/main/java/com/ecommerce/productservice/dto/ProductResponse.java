package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(String id, String skuCode, String name, String description, Instant createdAt, Instant updatedAt,
                              BigDecimal price) {
}
