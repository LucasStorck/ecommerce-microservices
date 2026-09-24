package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.InventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.QuantityRequest;
import com.ecommerce.inventoryservice.dto.StockResponse;
import com.ecommerce.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @PostMapping
  public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryRequest inventoryRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventory(inventoryRequest));
  }

  // Batch stock check used by order-service: GET /api/inventory?skuCode=A&skuCode=B
  @GetMapping
  public List<StockResponse> checkStock(@RequestParam("skuCode") List<String> skuCodes) {
    return inventoryService.checkStock(skuCodes);
  }

  @GetMapping("/{skuCode}")
  public InventoryResponse getInventoryBySkuCode(@PathVariable String skuCode) {
    return inventoryService.getInventoryBySkuCode(skuCode);
  }

  @PutMapping("/{skuCode}")
  public InventoryResponse updateQuantity(@PathVariable String skuCode,
                                          @Valid @RequestBody QuantityRequest quantityRequest) {
    return inventoryService.updateQuantity(skuCode, quantityRequest);
  }
}
