package com.ecommerce.orderservice.dto;

// Mirrors inventory-service's StockResponse. Duplicated on purpose: services share a
// contract (the JSON), not a class, so they can be built and deployed independently.
public record StockResponse(String skuCode, int quantity) {
}
