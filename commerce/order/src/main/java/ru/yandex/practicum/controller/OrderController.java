package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.order.CreateOrderRequest;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Order create(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<Order> getOrders(@RequestParam(required = false) UUID cartId) {
        return orderService.getOrders(cartId);
    }

    @PostMapping("/{id}/delivery-cost")
    public BigDecimal deliveryCost(@PathVariable UUID id) {
        return orderService.calculateDelivery(id);
    }

    @PostMapping("/{id}/total-cost")
    public BigDecimal totalCost(@PathVariable UUID id) {
        return orderService.calculateTotal(id);
    }

    @PostMapping("/{id}/assemble")
    public void assemble(@PathVariable UUID id) {
        orderService.assembleOrder(id);
    }

    @PostMapping("/{id}/pay")
    public UUID pay(@PathVariable UUID id) {
        return orderService.payOrder(id);
    }

    @PostMapping("/{id}/delivery")
    public UUID delivery(@PathVariable UUID id) {
        return orderService.createDelivery(id);
    }

    @PostMapping("/{id}/payment/success")
    public void paymentSuccess(@PathVariable UUID id) {
        orderService.paymentSuccess(id);
    }

    @PostMapping("/{id}/return")
    public void returnOrder(@PathVariable UUID id) {
        orderService.returnOrder(id);
    }

    @PostMapping("/{id}/payment/failed")
    public void paymentFailed(@PathVariable UUID id) {
        orderService.paymentFailed(id);
    }

    @PostMapping("/{id}/delivery/success")
    public void deliverySuccess(@PathVariable UUID id) {
        orderService.deliverySuccess(id);
    }

    @PostMapping("/{id}/delivery/failed")
    public void deliveryFailed(@PathVariable UUID id) {
        orderService.deliveryFailed(id);
    }

    @PostMapping("/{orderId}/product-cost")
    public BigDecimal productCost(@PathVariable UUID orderId) {
        return orderService.calculateProductsCost(orderId);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}