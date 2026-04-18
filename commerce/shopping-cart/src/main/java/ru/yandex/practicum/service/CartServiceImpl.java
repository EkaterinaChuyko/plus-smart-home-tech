package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartItemDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckResponseDto;
import ru.yandex.practicum.model.Cart;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.repository.CartRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    ;
    private final WarehouseClient warehouseClient;

    public CartServiceImpl(CartRepository cartRepository, WarehouseClient warehouseClient) {
        this.cartRepository = cartRepository;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public CartDto getCart(String username) {
        Cart cart = getOrCreateCart(username);
        return toDto(cart);
    }

    @Override
    public CartDto addItem(String username, CartItemDto itemDto) {

        WarehouseCheckRequestDto request = new WarehouseCheckRequestDto();
        request.setItems(List.of(itemDto));

        WarehouseCheckResponseDto response = warehouseClient.checkAvailability(request);

        if (!response.isAvailable(itemDto.getProductId())) {
            throw new RuntimeException("Недостаточно товара на складе");
        }

        Cart cart = getOrCreateCart(username);

        if (!cart.isActive()) {
            throw new RuntimeException("Корзина деактивирована");
        }

        Optional<CartItem> existingItem = cart.getItems().stream().filter(i -> i.getProductId().equals(itemDto.getProductId())).findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + itemDto.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setProductId(itemDto.getProductId());
            item.setQuantity(itemDto.getQuantity());
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Override
    public CartDto updateItem(String username, CartItemDto itemDto) {

        Cart cart = getOrCreateCart(username);

        if (!cart.isActive()) {
            throw new RuntimeException("Корзина деактивирована");
        }

        WarehouseCheckRequestDto request = new WarehouseCheckRequestDto();
        request.setItems(List.of(itemDto));

        WarehouseCheckResponseDto response = warehouseClient.checkAvailability(request);

        if (!response.isAvailable(itemDto.getProductId())) {
            throw new RuntimeException("Недостаточно товара на складе");
        }

        cart.getItems().forEach(item -> {
            if (item.getProductId().equals(itemDto.getProductId())) {
                item.setQuantity(itemDto.getQuantity());
            }
        });

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Override
    public void deactivateCart(String username) {
        Cart cart = getOrCreateCart(username);
        cart.setActive(false);
        cartRepository.save(cart);
    }

    @Override
    public void checkoutCart(String username) {
        Cart cart = cartRepository.findByUsernameAndActiveTrue(username).orElseThrow(() -> new RuntimeException("Активная корзина не найдена для пользователя " + username));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }

        WarehouseCheckRequestDto request = new WarehouseCheckRequestDto();
        request.setItems(cart.getItems().stream().map(item -> {
            CartItemDto dto = new CartItemDto();
            dto.setProductId(item.getProductId());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).toList());

        WarehouseCheckResponseDto response = warehouseClient.checkAvailability(request);

        List<Long> unavailableProducts = cart.getItems().stream().filter(item -> !response.isAvailable(item.getProductId())).map(CartItem::getProductId).toList();

        if (!unavailableProducts.isEmpty()) {
            throw new RuntimeException("Недостаточно товаров на складе: " + unavailableProducts);
        }

        cart.setActive(false);
        cartRepository.save(cart);

        System.out.println("Корзина пользователя " + username + " успешно оформлена");
    }

    private Cart getOrCreateCart(String username) {
        return cartRepository.findByUsernameAndActiveTrue(username).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUsername(username);
            return cartRepository.save(cart);
        });
    }

    private CartDto toDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setUsername(cart.getUsername());
        dto.setActive(cart.isActive());

        dto.setItems(cart.getItems().stream().map(item -> {
            CartItemDto i = new CartItemDto();
            i.setProductId(item.getProductId());
            i.setQuantity(item.getQuantity());
            return i;
        }).toList());

        return dto;
    }
}
