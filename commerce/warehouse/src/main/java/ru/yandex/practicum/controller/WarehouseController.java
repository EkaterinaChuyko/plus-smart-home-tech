package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.dto.order.OrderItemDto;
import ru.yandex.practicum.dto.warehouse.*;
import ru.yandex.practicum.service.WarehouseService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/item")
    public void addItem(@RequestBody WarehouseItemDto dto) {
        warehouseService.addItem(dto);
    }

    @PostMapping("/item/{productId}/quantity")
    public void updateQuantity(
            @PathVariable UUID productId,
            @RequestParam int quantity
    ) {
        warehouseService.updateQuantity(productId, quantity);
    }

    @PostMapping("/check")
    public WarehouseCheckResponseDto checkAvailability(@RequestBody WarehouseCheckRequestDto request) {
        log.debug("Check warehouse availability: {}", request);
        return warehouseService.checkAvailability(request);
    }

    @GetMapping("/address")
    public AddressDTO getWarehouseAddress() {
        return warehouseService.getCurrentAddress();
    }

    @PostMapping("/assemble")
    public void assemble(@RequestParam UUID orderId, @RequestBody List<OrderItemDto> items) {
        warehouseService.assemble(orderId, items);
    }

    @PostMapping("/shipped")
    public void shipped(@RequestParam UUID orderId, @RequestParam UUID deliveryId) {
        warehouseService.shippedToDelivery(orderId, deliveryId);
    }

    @PostMapping("/return")
    public void returnItems(@RequestBody List<OrderItemDto> items) {
        warehouseService.returnItems(items);
    }
}