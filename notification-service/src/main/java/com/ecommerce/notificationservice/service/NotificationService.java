package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.OrderPlacedEvent;
import com.ecommerce.notificationservice.model.Notification;

import java.util.List;

public interface NotificationService {

  void notifyOrderPlaced(OrderPlacedEvent event);

  List<Notification> getAllNotifications();
}
