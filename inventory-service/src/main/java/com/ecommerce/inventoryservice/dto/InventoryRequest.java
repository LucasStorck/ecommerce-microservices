package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryRequest(@NotBlank String skuCode, @NotNull @PositiveOrZero Integer quantity) {
}
