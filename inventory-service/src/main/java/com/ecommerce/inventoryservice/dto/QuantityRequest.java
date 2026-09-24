package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record QuantityRequest(@NotNull @PositiveOrZero Integer quantity) {
}
