package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.enums.ProductCategory;

import java.util.List;

public interface ProductService {
    List<ProductDto> getProducts(ProductCategory category);
    ProductDto getProduct(Long id);
    ProductDto addProduct(ProductDto product);
    ProductDto updateProduct(ProductDto product);
    void deactivateProduct(Long id);
}
