package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.product.ProductDto;
import ru.yandex.practicum.enums.ProductCategory;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "shopping-store", path = "/store/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ProductDto getProduct(@PathVariable UUID id);

    @GetMapping
    List<ProductDto> getProducts(@RequestParam(required = false) ProductCategory category);
}