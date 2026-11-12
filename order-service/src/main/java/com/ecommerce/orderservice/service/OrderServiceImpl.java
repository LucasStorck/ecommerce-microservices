package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.enums.Status;
import com.ecommerce.orderservice.exception.InvalidOrderStatusException;
import com.ecommerce.orderservice.model.OrderItem;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final InventoryClient inventoryClient;
  private final OrderEventProducer orderEventProducer;
  private final ProductClient productClient;

  public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper,
                          InventoryClient inventoryClient, ProductClient productClient,
                          OrderEventProducer orderEventProducer) {
    this.orderRepository = orderRepository;
    this.orderMapper = orderMapper;
    this.inventoryClient = inventoryClient;
    this.productClient = productClient;
    this.orderEventProducer = orderEventProducer;
  }

  // Deliberately not @Transactional: the inventory call is remote and can take seconds, and
  // a transaction would hold a DB connection the whole time. save() opens its own short
  // transaction, which is enough to persist the order and its items atomically.
  @Override
  public OrderResponse placeOrder(OrderRequest orderRequest) {
    // Prices first: an unknown sku or a product-service outage rejects the order before any
    // stock is reserved, so there is nothing to undo.
    Map<String, BigDecimal> prices = productClient.getPrices(distinctSkus(orderRequest));

    List<ReservationItem> reservation = toReservation(orderRequest);
    inventoryClient.reserve(reservation);

    Order savedOrder;
    try {
      Order order = orderMapper.toEntity(orderRequest);
      order.getOrderItems().forEach(item -> item.setPrice(prices.get(item.getSkuCode())));
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

  @Override
  public OrderResponse confirmOrder(String id) {
    transitionOrThrow(id, Status.CONFIRMED);
    return getOrderById(id);
  }

  // Not @Transactional, for the same reason as placeOrder (remote call to inventory). The status
  // flips first, atomically, so a double cancel can't release the same stock twice; if the
  // release then fails, the status is put back so the order and its reserved stock stay consistent.
  @Override
  public OrderResponse cancelOrder(String id) {
    Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    List<ReservationItem> reservation = reservationOf(order.getOrderItems().stream()
        .collect(Collectors.toMap(OrderItem::getSkuCode, OrderItem::getQuantity,
            Integer::sum, LinkedHashMap::new)));

    transitionOrThrow(id, Status.CANCELLED);
    try {
      inventoryClient.release(reservation);
    } catch (RuntimeException ex) {
      orderRepository.transition(id, Status.CANCELLED, Status.PENDING);
      throw ex;
    }
    return getOrderById(id);
  }

  // Only PENDING orders can move on. 0 rows updated means either no such order or it is no
  // longer PENDING; a second lookup tells the two apart for the right error.
  private void transitionOrThrow(String id, Status target) {
    if (orderRepository.transition(id, Status.PENDING, target) == 0) {
      Order current = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
      throw new InvalidOrderStatusException(id, current.getStatus(), target);
    }
  }

  private Set<String> distinctSkus(OrderRequest orderRequest) {
    return orderRequest.items().stream()
        .map(OrderItemRequest::skuCode)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  // The same skuCode may appear in more than one item, so quantities are summed per sku
  // before reserving; the reservation is atomic in inventory-service, so concurrent orders
  // can no longer oversell the same units.
  private List<ReservationItem> toReservation(OrderRequest orderRequest) {
    return reservationOf(orderRequest.items().stream()
        .collect(Collectors.toMap(OrderItemRequest::skuCode, OrderItemRequest::quantity,
            Integer::sum, LinkedHashMap::new)));
  }

  private List<ReservationItem> reservationOf(Map<String, Integer> quantityBySku) {
    return quantityBySku.entrySet().stream()
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
