package ru.yandex.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.address.AddressEmbeddable;
import ru.yandex.practicum.client.DeliveryClient;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.delivery.DeliveryRequest;
import ru.yandex.practicum.dto.order.CreateOrderRequest;
import ru.yandex.practicum.dto.order.OrderItemDto;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;
import ru.yandex.practicum.enums.OrderStatus;
import ru.yandex.practicum.mapper.OrderAddressMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderItem;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;
    private final DeliveryClient deliveryClient;
    private final OrderAddressMapper orderAddressMapper;

    public Order createOrder(CreateOrderRequest request) {

        List<OrderItem> items = request.getItems().stream().map(dto -> OrderItem.builder().productId(dto.getProductId()).quantity(dto.getQuantity()).build()).toList();

        AddressEmbeddable address = null;

        if (request.getDeliveryAddress() != null) {
            address = new AddressEmbeddable(request.getDeliveryAddress().getCountry(), request.getDeliveryAddress().getCity(), request.getDeliveryAddress().getStreet(), request.getDeliveryAddress().getHouse(), request.getDeliveryAddress().getFlat());
        }

        Order order = Order.builder().cartId(request.getCartId()).items(items).weight(request.getWeight()).volume(request.getVolume()).fragile(request.getFragile()).deliveryAddress(address).status(OrderStatus.NEW).build();

        return orderRepository.save(order);
    }

    public List<Order> getOrders(UUID cartId) {
        if (cartId == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByCartId(cartId);
    }

    public Double calculateProductsCost(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.ASSEMBLY_FAILED) {
            throw new IllegalStateException("Order assembly failed");
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Order has no items");
        }

        List<OrderItemDto> items = order.getItems().stream().map(item -> new OrderItemDto(item.getProductId(), item.getQuantity())).toList();

        Double cost = paymentClient.productCost(new OrderRequest(items));

        order.setProductsPrice(cost);
        orderRepository.save(order);

        return cost;
    }

    public Double calculateDelivery(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ASSEMBLED) {
            throw new IllegalStateException("Order must be ASSEMBLED before delivery calculation. Current status: " + order.getStatus());
        }

        if (order.getWeight() == null || order.getVolume() == null || order.getDeliveryAddress() == null) {
            throw new IllegalStateException("Order is missing required data for delivery calculation");
        }

        DeliveryRequest request = DeliveryRequest.builder().orderId(orderId).weight(order.getWeight()).volume(order.getVolume()).fragile(Boolean.TRUE.equals(order.getFragile())).deliveryAddress(orderAddressMapper.toDto(order.getDeliveryAddress())).build();

        try {
            Double cost = deliveryClient.calculateCost(request);
            order.setDeliveryPrice(cost);
            orderRepository.save(order);
            return cost;
        } catch (Exception e) {
            log.error("Delivery calculation failed", e);
            throw new RuntimeException(e);
        }
    }

    public Double calculateTotal(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.ASSEMBLED) {
            throw new IllegalStateException("Order is not ready for total calculation");
        }

        if (order.getProductsPrice() == null) {
            calculateProductsCost(orderId);
            order = getOrderOrThrow(orderId);
        }

        if (order.getDeliveryPrice() == null) {
            throw new IllegalStateException("Delivery cost not calculated");
        }

        Double total = paymentClient.getTotalCost(new TotalCostRequest(order.getProductsPrice(), order.getDeliveryPrice()));

        order.setTotalPrice(total);
        order.setStatus(OrderStatus.ON_PAYMENT);

        orderRepository.save(order);

        return total;
    }

    @Transactional
    public void assembleOrder(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Order is not in NEW status");
        }

        try {
            List<OrderItemDto> items = order.getItems().stream().map(item -> new OrderItemDto(item.getProductId(), item.getQuantity())).toList();
            warehouseClient.assemble(orderId, items);

            order.setStatus(OrderStatus.ASSEMBLED);

        } catch (Exception e) {
            log.error("Assembly failed", e);
            order.setStatus(OrderStatus.ASSEMBLY_FAILED);
        }

        orderRepository.save(order);
    }

    @Transactional
    public UUID payOrder(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ASSEMBLED) {
            throw new IllegalStateException("Order must be assembled before payment");
        }

        if (order.getTotalPrice() == null) {
            throw new IllegalStateException("Total cost not calculated");
        }

        UUID paymentId = paymentClient.payment(new PaymentRequest(orderId, order.getProductsPrice(), order.getDeliveryPrice(), order.getTotalPrice()));

        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.ON_PAYMENT);
        orderRepository.save(order);

        return paymentId;
    }

    @Transactional
    public void paymentSuccess(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ON_PAYMENT) {
            throw new IllegalStateException("Invalid state for payment success");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Transactional
    public void paymentFailed(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ON_PAYMENT) {
            throw new IllegalStateException("Invalid state for payment fail");
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
    }

    @Transactional
    public UUID createDelivery(UUID orderId) {

        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order is not paid");
        }

        AddressDTO warehouseAddress = warehouseClient.getWarehouseAddress();

        String warehouseAddressString = warehouseAddress.getCountry() + ", " + warehouseAddress.getCity() + ", " + warehouseAddress.getStreet();

        DeliveryRequest request = DeliveryRequest.builder().orderId(orderId).weight(order.getWeight()).volume(order.getVolume()).fragile(order.getFragile()).warehouseAddress(warehouseAddressString).deliveryAddress(OrderAddressMapper.toDto(order.getDeliveryAddress())).build();

        UUID deliveryId = deliveryClient.planDelivery(request);

        order.setDeliveryId(deliveryId);
        order.setStatus(OrderStatus.ON_DELIVERY);

        orderRepository.save(order);

        return deliveryId;
    }

    @Transactional
    public void deliverySuccess(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ON_DELIVERY) {
            throw new IllegalStateException("Invalid state for delivery success");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Transactional
    public void deliveryFailed(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ON_DELIVERY) {
            throw new IllegalStateException("Invalid state for delivery fail");
        }

        order.setStatus(OrderStatus.DELIVERY_FAILED);
        orderRepository.save(order);
    }

    @Transactional
    public void returnOrder(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be returned");
        }

        order.setStatus(OrderStatus.PRODUCT_RETURNED);
        orderRepository.save(order);
    }

    public Order getOrder(UUID id) {
        return getOrderOrThrow(id);
    }

    private Order getOrderOrThrow(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }
}