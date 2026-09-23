package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.InventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.QuantityRequest;
import com.ecommerce.inventoryservice.dto.StockResponse;

import java.util.List;

public interface InventoryService {
  InventoryResponse createInventory(InventoryRequest inventoryRequest);
  InventoryResponse getInventoryBySkuCode(String skuCode);
  InventoryResponse updateQuantity(String skuCode, QuantityRequest quantityRequest);
  List<StockResponse> checkStock(List<String> skuCodes);
}
