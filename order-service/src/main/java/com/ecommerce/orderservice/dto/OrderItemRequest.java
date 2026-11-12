package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// No price here: it comes from the catalog (product-service), never from the client.
public record OrderItemRequest(@NotBlank String skuCode, @NotNull @Positive Integer quantity) {
}
