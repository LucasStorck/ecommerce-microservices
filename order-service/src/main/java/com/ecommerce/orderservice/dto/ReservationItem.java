package com.ecommerce.orderservice.dto;

// Mirrors inventory-service's ReservationItem. Duplicated on purpose: services share a
// contract (the JSON), not a class, so they can be built and deployed independently.
public record ReservationItem(String skuCode, int quantity) {
}
