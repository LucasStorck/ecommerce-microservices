package com.ecommerce.orderservice.model;

import com.ecommerce.orderservice.enums.Status;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;
  @Column(name = "order_number")
  private String orderNumber;
  @CreatedDate
  private Instant ordered;
  @Enumerated(EnumType.STRING)
  private Status status;
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> orderItems;

  public Order() {
  }

  public Order(String orderNumber, List<OrderItem> orderItems) {
    this.orderNumber = orderNumber;
    this.orderItems = orderItems;
  }

  public String getId() {
    return id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public Instant getOrdered() {
    return ordered;
  }

  public void setOrdered(Instant ordered) {
    this.ordered = ordered;
  }

  public List<OrderItem> getOrderItems() {
    return orderItems;
  }

  public void setOrderItems(List<OrderItem> orderItems) {
    this.orderItems = orderItems;
  }
}
