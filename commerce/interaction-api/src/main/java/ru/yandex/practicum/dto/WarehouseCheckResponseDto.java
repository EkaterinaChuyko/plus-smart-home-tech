package ru.yandex.practicum.dto;

import java.util.Map;

import java.util.HashMap;

public class WarehouseCheckResponseDto {

    private Map<Long, Boolean> availability;

    public WarehouseCheckResponseDto() {
        this.availability = new HashMap<>();
    }

    public WarehouseCheckResponseDto(Map<Long, Boolean> availability) {
        this.availability = availability;
    }

    public Map<Long, Boolean> getAvailability() {
        return availability;
    }

    public void setAvailability(Map<Long, Boolean> availability) {
        this.availability = availability;
    }

    public boolean isAvailable(Long productId) {
        return availability.getOrDefault(productId, false);
    }

    public void addAvailability(Long productId, boolean available) {
        availability.put(productId, available);
    }
}
