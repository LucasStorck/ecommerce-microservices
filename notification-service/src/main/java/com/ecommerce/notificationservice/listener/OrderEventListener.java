package com.ecommerce.notificationservice.listener;

import com.ecommerce.notificationservice.event.OrderPlacedEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

  private final NotificationService notificationService;

  public OrderEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @KafkaListener(topics = "order-placed-events")
  public void onOrderPlaced(OrderPlacedEvent event) {
    notificationService.notifyOrderPlaced(event);
  }
}
