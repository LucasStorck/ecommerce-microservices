package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

// Only the part of product-service's ProductResponse that order-service needs; unknown JSON
// fields are ignored, so product-service can grow its response without breaking this client.
public record ProductPriceResponse(String skuCode, BigDecimal price) {
}
