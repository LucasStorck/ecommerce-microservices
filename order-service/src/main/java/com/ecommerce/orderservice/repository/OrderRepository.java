package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

  // orderItems is LAZY; fetching it in the same query avoids N+1 when mapping to the response.
  @Override
  @EntityGraph(attributePaths = "orderItems")
  List<Order> findAll();

  @Override
  @EntityGraph(attributePaths = "orderItems")
  Optional<Order> findById(String id);
}
