package ru.yandex.practicum.dto.warehouse;

import ru.yandex.practicum.dto.cart.CartItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarehouseCheckRequestDto {
    private List<CartItemDto> items;

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    public void addItem(UUID productId, int quantity) {
        if (items == null) items = new ArrayList<>();
        items.add(new CartItemDto(productId, quantity));
    }
}
