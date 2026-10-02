package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

  Optional<Inventory> findBySkuCode(String skuCode);

  boolean existsBySkuCode(String skuCode);

  List<Inventory> findBySkuCodeIn(Collection<String> skuCodes);

  // The stock check and the decrement are one SQL statement, so the database decides
  // atomically: two concurrent reservations can never both succeed on the last units.
  // Returns the number of rows changed: 0 means unknown sku or not enough stock.
  @Modifying
  @Query("update Inventory i set i.quantity = i.quantity - :quantity "
      + "where i.skuCode = :skuCode and i.quantity >= :quantity")
  int decrementIfAvailable(@Param("skuCode") String skuCode, @Param("quantity") int quantity);

  @Modifying
  @Query("update Inventory i set i.quantity = i.quantity + :quantity where i.skuCode = :skuCode")
  int increment(@Param("skuCode") String skuCode, @Param("quantity") int quantity);
}
