package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.enums.Status;
import com.ecommerce.orderservice.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

  // Compare-and-set on the status: the database decides atomically, so two concurrent
  // requests (e.g. cancelling twice) cannot both win. Returns 1 if this call made the change.
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("update Order o set o.status = :to where o.id = :id and o.status = :from")
  int transition(@Param("id") String id, @Param("from") Status from, @Param("to") Status to);
}
