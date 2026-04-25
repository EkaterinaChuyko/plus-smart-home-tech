package ru.yandex.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.address.WarehouseAddress;
import ru.yandex.practicum.dto.order.OrderItemDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckResponseDto;
import ru.yandex.practicum.dto.warehouse.WarehouseItemDto;
import ru.yandex.practicum.model.OrderBooking;
import ru.yandex.practicum.model.WarehouseItem;
import ru.yandex.practicum.repository.OrderBookingRepository;
import ru.yandex.practicum.repository.WarehouseRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository repository;
    private final WarehouseAddress warehouseAddress;
    private final OrderBookingRepository orderBookingRepository;

    @Override
    public void addItem(WarehouseItemDto dto) {
        WarehouseItem item = new WarehouseItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setWeight(dto.getWeight());
        item.setWidth(dto.getWidth());
        item.setHeight(dto.getHeight());
        item.setDepth(dto.getDepth());
        item.setFragile(dto.getFragile());

        repository.save(item);
    }

    @Override
    public void updateQuantity(UUID productId, int quantity) {
        WarehouseItem item = repository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        item.setQuantity(quantity);
        repository.save(item);
    }

    @Override
    public WarehouseCheckResponseDto checkAvailability(WarehouseCheckRequestDto request) {

        WarehouseCheckResponseDto response = new WarehouseCheckResponseDto();

        request.getItems().forEach(item -> {

            WarehouseItem warehouseItem = repository.findByProductId(item.getProductId())
                    .orElse(null);

            int quantity = (warehouseItem != null)
                    ? warehouseItem.getQuantity()
                    : 0;

            response.addProduct(item.getProductId(), quantity);
        });

        return response;
    }

    @Override
    public AddressDTO getCurrentAddress() {
        return warehouseAddress.getAddress();
    }

    @Override
    @Transactional
    public void assemble(UUID orderId, List<OrderItemDto> items) {

        for (OrderItemDto item : items) {

            WarehouseItem warehouseItem = repository.findByProductId(item.getProductId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Product not found: " + item.getProductId())
                    );

            if (warehouseItem.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Not enough stock for product: " + item.getProductId()
                );
            }

            warehouseItem.setQuantity(
                    warehouseItem.getQuantity() - item.getQuantity()
            );

            repository.save(warehouseItem);
        }
    }

    @Override
    @Transactional
    public void shippedToDelivery(UUID orderId, UUID deliveryId) {

        OrderBooking booking = orderBookingRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking not found for order: " + orderId
                ));

        booking.setDeliveryId(deliveryId);
    }

    @Override
    @Transactional
    public void returnItems(List<OrderItemDto> items) {
        for (OrderItemDto item : items) {
            WarehouseItem warehouseItem = repository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + item.getProductId()
                    ));

            warehouseItem.setQuantity(
                    warehouseItem.getQuantity() + item.getQuantity()
            );
        }
    }
}