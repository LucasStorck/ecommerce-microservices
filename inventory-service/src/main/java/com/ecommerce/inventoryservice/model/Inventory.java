package com.ecommerce.inventoryservice.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;
  @Column(name = "sku_code", nullable = false, unique = true)
  private String skuCode;
  @Column(nullable = false)
  private Integer quantity;
  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  public Inventory() {
  }

  public Inventory(String skuCode, Integer quantity) {
    this.skuCode = skuCode;
    this.quantity = quantity;
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

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
