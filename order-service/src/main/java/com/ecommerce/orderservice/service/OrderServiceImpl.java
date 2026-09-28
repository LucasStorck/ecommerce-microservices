package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.StockResponse;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final InventoryClient inventoryClient;

  public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper,
                          InventoryClient inventoryClient) {
    this.orderRepository = orderRepository;
    this.orderMapper = orderMapper;
    this.inventoryClient = inventoryClient;
  }

  // Deliberately not @Transactional: the inventory call is remote and can take seconds, and
  // a transaction would hold a DB connection the whole time. save() opens its own short
  // transaction, which is enough to persist the order and its items atomically.
  @Override
  public OrderResponse placeOrder(OrderRequest orderRequest) {
    verifyStock(orderRequest);
    Order order = orderMapper.toEntity(orderRequest);
    order.setOrderNumber(UUID.randomUUID().toString());
    return orderMapper.toResponse(orderRepository.save(order));
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrderResponse> getAllOrders() {
    return orderRepository.findAll().stream()
        .map(orderMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public OrderResponse getOrderById(String id) {
    return orderRepository.findById(id)
        .map(orderMapper::toResponse)
        .orElseThrow(() -> new OrderNotFoundException(id));
  }

  // The same skuCode may appear in more than one item, so quantities are summed per sku
  // before comparing; otherwise two items of 3 could each pass against a stock of 5.
  // This only checks: stock is not reserved, so two concurrent orders can still both pass.
  private void verifyStock(OrderRequest orderRequest) {
    Map<String, Integer> requested = orderRequest.items().stream()
        .collect(Collectors.toMap(OrderItemRequest::skuCode, OrderItemRequest::quantity,
            Integer::sum, LinkedHashMap::new));

    Map<String, Integer> available = inventoryClient.checkStock(List.copyOf(requested.keySet())).stream()
        .collect(Collectors.toMap(StockResponse::skuCode, StockResponse::quantity));

    List<String> insufficient = requested.entrySet().stream()
        .filter(entry -> available.getOrDefault(entry.getKey(), 0) < entry.getValue())
        .map(Map.Entry::getKey)
        .toList();

    if (!insufficient.isEmpty()) {
      throw new InsufficientStockException(insufficient);
    }
  }
}
