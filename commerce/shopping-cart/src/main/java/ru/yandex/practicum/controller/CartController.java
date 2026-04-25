package ru.yandex.practicum.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.cart.CartDto;
import ru.yandex.practicum.dto.cart.CartItemDto;
import ru.yandex.practicum.service.CartService;

@Slf4j
@RestController
@RequestMapping("/shopping-cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<CartDto> addProductToCart(@RequestParam String username, @RequestBody CartItemDto item) {

        return ResponseEntity.ok(cartService.addItem(username, item));
    }

    @GetMapping("/{username}")
    public ResponseEntity<CartDto> getCart(@PathVariable String username) {
        return ResponseEntity.ok(cartService.getCart(username));
    }

    @PutMapping("/update")
    public ResponseEntity<CartDto> updateItem(@RequestParam String username, @RequestBody CartItemDto item) {

        return ResponseEntity.ok(cartService.updateItem(username, item));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateCart(@RequestParam String username) {
        cartService.deactivateCart(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestParam String username) {
        cartService.checkoutCart(username); // логика оформления корзины
        return ResponseEntity.ok("Checkout successful");
    }

    @GetMapping
    public ResponseEntity<CartDto> getCartForDefaultUser() {
        log.info("Новый запрос на получение корзины");
        String defaultUsername = "defaultUser";
        return ResponseEntity.ok(cartService.getCart(defaultUsername));
    }

    @PostMapping("/{username}/item")
    public CartDto addItem(
            @PathVariable String username,
            @RequestBody CartItemDto itemDto
    ) {
        return cartService.addItem(username, itemDto);
    }
}
