package com.ecommerce.orderservice.dto;

import java.util.List;

public record ReservationRequest(List<ReservationItem> items) {
}
