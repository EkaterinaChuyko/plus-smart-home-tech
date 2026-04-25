package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.product.ProductDto;
import ru.yandex.practicum.enums.ProductCategory;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<ProductDto> getProducts(ProductCategory category);

    ProductDto getProduct(UUID id);

    ProductDto addProduct(ProductDto product);

    ProductDto updateProduct(ProductDto product);

    void deactivateProduct(UUID id);
}
