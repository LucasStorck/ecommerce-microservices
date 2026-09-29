package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.model.Notification;
import com.ecommerce.notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/api/notifications")
  public List<Notification> getAllNotifications() {
    return notificationService.getAllNotifications();
  }
}
