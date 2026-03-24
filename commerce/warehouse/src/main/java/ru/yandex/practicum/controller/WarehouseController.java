package ru.yandex.practicum.controller;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.api.WarehouseApi;
import ru.yandex.practicum.dto.WarehouseAddressDto;
import ru.yandex.practicum.dto.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.WarehouseCheckResponseDto;
import ru.yandex.practicum.dto.WarehouseItemDto;
import ru.yandex.practicum.service.WarehouseService;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController implements WarehouseApi {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Override
    public void addItem(WarehouseItemDto dto) {
        warehouseService.addItem(dto);
    }

    @Override
    public void updateQuantity(Long productId, int quantity) {
        warehouseService.updateQuantity(productId, quantity);
    }

    @Override
    public WarehouseCheckResponseDto checkAvailability(WarehouseCheckRequestDto request) {
        return warehouseService.checkAvailability(request);
    }

    @Override
    public WarehouseAddressDto getCurrentAddress() {
        return warehouseService.getCurrentAddress();
    }
}
