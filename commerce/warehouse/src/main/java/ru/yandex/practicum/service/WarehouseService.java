package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.WarehouseAddressDto;
import ru.yandex.practicum.dto.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.WarehouseCheckResponseDto;
import ru.yandex.practicum.dto.WarehouseItemDto;

public interface WarehouseService {

    void addItem(WarehouseItemDto dto);

    void updateQuantity(Long productId, int quantity);

    WarehouseCheckResponseDto checkAvailability(WarehouseCheckRequestDto request);

    WarehouseAddressDto getCurrentAddress();
}
