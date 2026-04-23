package ru.yandex.practicum.api;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.warehouse.WarehouseAddressDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckResponseDto;
import ru.yandex.practicum.dto.warehouse.WarehouseItemDto;

public interface WarehouseApi {

    @GetMapping("/address")
    WarehouseAddressDto getCurrentAddress();

    @PostMapping("/check")
    WarehouseCheckResponseDto checkAvailability(@RequestBody WarehouseCheckRequestDto request);

    @PostMapping("/add")
    void addItem(@RequestBody WarehouseItemDto dto);

    @PostMapping("/update")
    void updateQuantity(@RequestParam Long productId, @RequestParam int quantity);

}