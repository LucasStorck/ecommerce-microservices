package com.ecommerce.inventoryservice.dto;

import java.time.Instant;

public record InventoryResponse(String skuCode, Integer quantity, Instant createdAt, Instant updatedAt) {
}
