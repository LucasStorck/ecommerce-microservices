package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderPlacedEvent;
import com.ecommerce.notificationservice.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationServiceImpl implements NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

  // In-memory only: this service owns no database, so notifications live for the process's
  // lifetime and reset on restart. CopyOnWriteArrayList because the Kafka listener thread
  // writes while REST request threads read concurrently. A real notifier would call an
  // email/SMS provider here instead of logging.
  private final List<Notification> notifications = new CopyOnWriteArrayList<>();

  @Override
  public void notifyOrderPlaced(OrderPlacedEvent event) {
    Notification notification = new Notification(event.orderNumber(), event.ordered(), event.total(),
        event.items().size(), Instant.now());
    notifications.add(notification);
    log.info("Notifying customer: order {} placed, {} item(s), total {}", event.orderNumber(),
        event.items().size(), event.total());
  }

  @Override
  public List<Notification> getAllNotifications() {
    return List.copyOf(notifications);
  }
}
