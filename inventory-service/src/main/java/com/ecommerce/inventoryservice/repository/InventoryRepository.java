package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

  Optional<Inventory> findBySkuCode(String skuCode);

  boolean existsBySkuCode(String skuCode);

  List<Inventory> findBySkuCodeIn(Collection<String> skuCodes);
}
