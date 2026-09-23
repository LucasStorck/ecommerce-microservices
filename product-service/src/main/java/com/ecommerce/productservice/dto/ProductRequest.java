package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(@NotBlank String skuCode, @NotBlank String name, @NotBlank String description,
                             @NotNull @Positive BigDecimal price) {
}
