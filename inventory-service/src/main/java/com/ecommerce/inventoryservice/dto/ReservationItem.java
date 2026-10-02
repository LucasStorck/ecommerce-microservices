package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationItem(@NotBlank String skuCode, @NotNull @Positive Integer quantity) {
}
