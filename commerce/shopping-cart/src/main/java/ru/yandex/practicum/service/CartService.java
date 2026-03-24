package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.CartDto;
import ru.yandex.practicum.dto.CartItemDto;

public interface CartService {
    CartDto getCart(String username);

    CartDto addItem(String username, CartItemDto item);

    CartDto updateItem(String username, CartItemDto item);

    void deactivateCart(String username);
}
