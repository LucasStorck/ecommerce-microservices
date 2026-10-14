package com.ecommerce.orderservice.dto;

import java.util.List;

// Body of inventory-service's 409 on reserve (same contract-not-class rule as above).
public record StockShortageResponse(String message, List<String> insufficientSkus) {
}
