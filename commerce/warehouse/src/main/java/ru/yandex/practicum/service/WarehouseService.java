package ru.yandex.practicum.service;

import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.dto.order.OrderItemDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckResponseDto;
import ru.yandex.practicum.dto.warehouse.WarehouseItemDto;

import java.util.List;
import java.util.UUID;

public interface WarehouseService {

    void addItem(WarehouseItemDto dto);

    void updateQuantity(UUID productId, int quantity);

    WarehouseCheckResponseDto checkAvailability(WarehouseCheckRequestDto request);

    AddressDTO getCurrentAddress();

    void assemble(UUID orderId, List<OrderItemDto> items);

    void shippedToDelivery(UUID orderId, UUID deliveryId);

    void returnItems(List<OrderItemDto> items);
}