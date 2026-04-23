package ru.yandex.practicum.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.dto.product.ProductDto;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductStatus;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductDto> getProducts(ProductCategory category) {
        List<Product> products;

        if (category != null) {
            products = repository.findByCategoryAndStatus(category, ProductStatus.ACTIVE);
        } else {
            products = repository.findByStatus(ProductStatus.ACTIVE);
        }

        return products.stream().map(this::toDto).toList();
    }

    @Override
    public ProductDto getProduct(UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found: " + id
                ));

        return toDto(product);
    }

    @Override
    public ProductDto addProduct(ProductDto dto) {
        Product product = toEntity(dto);
        product.setStatus(ProductStatus.ACTIVE);
        return toDto(repository.save(product));
    }

    @Override
    public ProductDto updateProduct(ProductDto dto) {
        Product product = repository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setAvailability(dto.getAvailability());
        product.setImages(dto.getImages());
        product.setPrice(dto.getPrice());

        return toDto(repository.save(product));
    }

    @Override
    public void deactivateProduct(UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStatus(ProductStatus.DEACTIVATE);
        repository.save(product);
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategory(product.getCategory());
        dto.setAvailability(product.getAvailability());
        dto.setStatus(product.getStatus());
        dto.setImages(product.getImages());
        dto.setPrice(product.getPrice());
        return dto;
    }

    private Product toEntity(ProductDto dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setAvailability(dto.getAvailability());
        product.setImages(dto.getImages());
        product.setPrice(dto.getPrice());
        return product;
    }
}