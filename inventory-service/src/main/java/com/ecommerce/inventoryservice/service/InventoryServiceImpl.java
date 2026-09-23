package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.InventoryRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.QuantityRequest;
import com.ecommerce.inventoryservice.dto.StockResponse;
import com.ecommerce.inventoryservice.exception.InventoryAlreadyExistsException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryRepository inventoryRepository;
  private final InventoryMapper inventoryMapper;

  public InventoryServiceImpl(InventoryRepository inventoryRepository, InventoryMapper inventoryMapper) {
    this.inventoryRepository = inventoryRepository;
    this.inventoryMapper = inventoryMapper;
  }

  @Override
  @Transactional
  public InventoryResponse createInventory(InventoryRequest inventoryRequest) {
    if (inventoryRepository.existsBySkuCode(inventoryRequest.skuCode())) {
      throw new InventoryAlreadyExistsException(inventoryRequest.skuCode());
    }
    Inventory inventory = inventoryMapper.toEntity(inventoryRequest);
    return inventoryMapper.toResponse(inventoryRepository.save(inventory));
  }

  @Override
  @Transactional(readOnly = true)
  public InventoryResponse getInventoryBySkuCode(String skuCode) {
    return inventoryMapper.toResponse(findOrThrow(skuCode));
  }

  @Override
  @Transactional
  public InventoryResponse updateQuantity(String skuCode, QuantityRequest quantityRequest) {
    Inventory inventory = findOrThrow(skuCode);
    inventory.setQuantity(quantityRequest.quantity());
    // Flush so @LastModifiedDate (set on JPA's pre-update, at flush time) is in the response.
    return inventoryMapper.toResponse(inventoryRepository.saveAndFlush(inventory));
  }

  /**
   * Returns one entry per requested skuCode, in request order. A skuCode with
   * no inventory record is reported with quantity 0 rather than omitted, so
   * the caller never has to guess what a missing entry means.
   */
  @Override
  @Transactional(readOnly = true)
  public List<StockResponse> checkStock(List<String> skuCodes) {
    Map<String, Integer> quantities = inventoryRepository.findBySkuCodeIn(skuCodes).stream()
        .collect(Collectors.toMap(Inventory::getSkuCode, Inventory::getQuantity));

    return skuCodes.stream()
        .distinct()
        .map(skuCode -> new StockResponse(skuCode, quantities.getOrDefault(skuCode, 0)))
        .toList();
  }

  private Inventory findOrThrow(String skuCode) {
    return inventoryRepository.findBySkuCode(skuCode)
        .orElseThrow(() -> new InventoryNotFoundException(skuCode));
  }
}
