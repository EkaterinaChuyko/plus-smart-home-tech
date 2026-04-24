package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;
import ru.yandex.practicum.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/products-cost")
    public BigDecimal getProductsCost(@RequestBody OrderRequest request) {
        return service.calculateProductsCost(request);
    }

    @PostMapping("/total")
    public BigDecimal getTotalCost(@RequestBody TotalCostRequest request) {
        return service.calculateTotalCost(request);
    }

    @PostMapping("/pay")
    public UUID createPayment(@RequestBody PaymentRequest request) {
        return service.createPayment(request);
    }

    @PostMapping("/{id}/success")
    public void success(@PathVariable UUID id) {
        service.success(id);
    }

    @PostMapping("/{id}/failed")
    public void failed(@PathVariable UUID id) {
        service.failed(id);
    }
}