package com.ecommerce.orderservice.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;
  @Column(name = "sku_code")
  private String skuCode;
  @Column(precision = 10, scale = 2)
  private BigDecimal price;
  private Integer quantity;
  @ManyToOne
  @JoinColumn(name = "order_id")
  private Order order;

  public OrderItem() {
  }

  public OrderItem(String skuCode, BigDecimal price, Integer quantity, Order order) {
    this.skuCode = skuCode;
    this.price = price;
    this.quantity = quantity;
    this.order = order;
  }

  public String getId() {
    return id;
  }

  public String getSkuCode() {
    return skuCode;
  }

  public void setSkuCode(String skuCode) {
    this.skuCode = skuCode;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }
}
