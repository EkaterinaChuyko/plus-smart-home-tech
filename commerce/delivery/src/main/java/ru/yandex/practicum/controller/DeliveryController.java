package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.delivery.DeliveryRequest;
import ru.yandex.practicum.service.DeliveryService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService service;

    @PostMapping("/cost")
    public BigDecimal calculateCost(@RequestBody DeliveryRequest request) {
        return service.calculateCost(request);
    }

    @PostMapping
    public UUID planDelivery(@RequestBody DeliveryRequest request) {
        return service.planDelivery(request);
    }

    @PostMapping("/{id}/start")
    public void start(@PathVariable UUID id) {
        service.startDelivery(id);
    }

    @PostMapping("/{id}/success")
    public void success(@PathVariable UUID id) {
        service.completeDelivery(id);
    }

    @PostMapping("/{id}/fail")
    public void fail(@PathVariable UUID id) {
        service.failDelivery(id);
    }

}