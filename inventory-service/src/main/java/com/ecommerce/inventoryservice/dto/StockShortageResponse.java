package com.ecommerce.inventoryservice.dto;

import java.util.List;

// Body of the 409 returned by reserve: tells the caller exactly which skus fell short.
public record StockShortageResponse(String message, List<String> insufficientSkus) {
}
