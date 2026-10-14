package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ReservationItem;
import com.ecommerce.orderservice.event.OrderEventProducer;
import com.ecommerce.orderservice.event.OrderItemEvent;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final InventoryClient inventoryClient;
  private final OrderEventProducer orderEventProducer;

  public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper,
                          InventoryClient inventoryClient, OrderEventProducer orderEventProducer) {
    this.orderRepository = orderRepository;
    this.orderMapper = orderMapper;
    this.inventoryClient = inventoryClient;
    this.orderEventProducer = orderEventProducer;
  }

  // Deliberately not @Transactional: the inventory call is remote and can take seconds, and
  // a transaction would hold a DB connection the whole time. save() opens its own short
  // transaction, which is enough to persist the order and its items atomically.
  @Override
  public OrderResponse placeOrder(OrderRequest orderRequest) {
    List<ReservationItem> reservation = toReservation(orderRequest);
    inventoryClient.reserve(reservation);

    Order savedOrder;
    try {
      Order order = orderMapper.toEntity(orderRequest);
      order.setOrderNumber(UUID.randomUUID().toString());
      savedOrder = orderRepository.save(order);
    } catch (RuntimeException ex) {
      releaseQuietly(reservation);
      throw ex;
    }
    publishOrderPlaced(savedOrder);
    return orderMapper.toResponse(savedOrder);
  }

  private void publishOrderPlaced(Order order) {
    List<OrderItemEvent> items = order.getOrderItems().stream()
        .map(item -> new OrderItemEvent(item.getSkuCode(), item.getQuantity()))
        .toList();
    OrderPlacedEvent event = new OrderPlacedEvent(order.getOrderNumber(), order.getOrdered(),
        orderMapper.calculateTotal(order), items);
    orderEventProducer.publishOrderPlaced(event);
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
  // before reserving; the reservation is atomic in inventory-service, so concurrent orders
  // can no longer oversell the same units.
  private List<ReservationItem> toReservation(OrderRequest orderRequest) {
    return orderRequest.items().stream()
        .collect(Collectors.toMap(OrderItemRequest::skuCode, OrderItemRequest::quantity,
            Integer::sum, LinkedHashMap::new))
        .entrySet().stream()
        .map(entry -> new ReservationItem(entry.getKey(), entry.getValue()))
        .toList();
  }

  // Compensation must not mask the original failure. If it fails too, the stock stays
  // reserved without an order: logged so it can be reconciled by hand.
  private void releaseQuietly(List<ReservationItem> reservation) {
    try {
      inventoryClient.release(reservation);
    } catch (RuntimeException releaseFailure) {
      log.error("Could not release reservation {} after order save failed; stock is leaked",
          reservation, releaseFailure);
    }
  }
}
