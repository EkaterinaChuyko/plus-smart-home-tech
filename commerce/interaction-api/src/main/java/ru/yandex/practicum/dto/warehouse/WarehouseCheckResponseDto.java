package ru.yandex.practicum.dto.warehouse;

import java.util.Map;

import java.util.HashMap;
import java.util.UUID;

public class WarehouseCheckResponseDto {

    private Map<UUID, Integer> availableQuantity;

    public WarehouseCheckResponseDto() {
        this.availableQuantity = new HashMap<>();
    }

    public WarehouseCheckResponseDto(Map<UUID, Integer> availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Map<UUID, Integer> getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Map<UUID, Integer> availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public boolean isAvailable(UUID productId, int requestedQty) {
        return availableQuantity.getOrDefault(productId, 0) >= requestedQty;
    }

    public void addProduct(UUID productId, int quantity) {
        availableQuantity.put(productId, quantity);
    }
}