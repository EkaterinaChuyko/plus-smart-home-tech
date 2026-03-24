package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.address.WarehouseAddress;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.model.WarehouseItem;
import ru.yandex.practicum.repository.WarehouseRepository;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository repository;
    private final WarehouseAddress warehouseAddress;

    public WarehouseServiceImpl(WarehouseRepository repository,
                                WarehouseAddress warehouseAddress) {
        this.repository = repository;
        this.warehouseAddress = warehouseAddress;
    }

    @Override
    public void addItem(WarehouseItemDto dto) {
        WarehouseItem item = new WarehouseItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setWeight(dto.getWeight());
        item.setWidth(dto.getWidth());
        item.setHeight(dto.getHeight());
        item.setDepth(dto.getDepth());
        item.setFragile(dto.isFragile());

        repository.save(item);
    }

    @Override
    public void updateQuantity(Long productId, int quantity) {
        WarehouseItem item = repository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        item.setQuantity(quantity);
        repository.save(item);
    }

    @Override
    public WarehouseCheckResponseDto checkAvailability(WarehouseCheckRequestDto request) {
        WarehouseCheckResponseDto response = new WarehouseCheckResponseDto();

        for (CartItemDto item : request.getItems()) {
            WarehouseItem warehouseItem = repository.findByProductId(item.getProductId())
                    .orElse(null);

            boolean available = warehouseItem != null &&
                                warehouseItem.getQuantity() >= item.getQuantity();

            response.addAvailability(item.getProductId(), available);
        }

        return response;
    }

    @Override
    public WarehouseAddressDto getCurrentAddress() {
        return warehouseAddress.getAddress();
    }
}
