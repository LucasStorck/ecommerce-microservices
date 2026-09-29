package com.ecommerce.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

  public static final String ORDER_PLACED_TOPIC = "order-placed-events";

  private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

  private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

  public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  // Keyed by orderNumber so any future event for the same order lands on the same partition
  // and stays in order. send() returns a future and does not block: placeOrder does not wait
  // on Kafka, matching the "order -> notification is asynchronous" decision.
  public void publishOrderPlaced(OrderPlacedEvent event) {
    kafkaTemplate.send(ORDER_PLACED_TOPIC, event.orderNumber(), event)
        .whenComplete((result, exception) -> {
          if (exception != null) {
            log.warn("Failed to publish order-placed event for order {}", event.orderNumber(), exception);
          } else {
            log.info("Published order-placed event for order {}", event.orderNumber());
          }
        });
  }
}
