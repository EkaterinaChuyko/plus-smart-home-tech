package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.delivery.DeliveryRequest;
import ru.yandex.practicum.enums.DeliveryStatus;
import ru.yandex.practicum.exception.DeliveryCalculationException;
import ru.yandex.practicum.mapper.AddressMapper;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryService {

    private final DeliveryRepository repository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;
    private final AddressMapper addressMapper;

    private static final double BASE_COST = 5.0;
    private static final String ADDRESS_1 = "ADDRESS_1";
    private static final String ADDRESS_2 = "ADDRESS_2";

    public Double calculateCost(DeliveryRequest request) {

        validateRequest(request);

        try {
            double cost = BASE_COST;

            AddressDTO warehouseAddress = warehouseClient.getWarehouseAddress();

            if (warehouseAddress == null) {
                throw new DeliveryCalculationException("Warehouse address is null", null);
            }

            String warehouseFullAddress = warehouseAddress.getCountry() + " " + warehouseAddress.getCity() + " " + warehouseAddress.getStreet();

            if (warehouseFullAddress.contains(ADDRESS_1)) {
                cost *= 1;
            } else if (warehouseFullAddress.contains(ADDRESS_2)) {
                cost *= 2;
            }

            cost += BASE_COST;

            if (Boolean.TRUE.equals(request.getFragile())) {
                cost += cost * 0.2;
            }

            cost += request.getWeight() * 0.3;

            cost += request.getVolume() * 0.2;

            String deliveryStreet = request.getDeliveryAddress().getStreet();

            if (deliveryStreet != null && !warehouseFullAddress.toLowerCase().contains(deliveryStreet.toLowerCase())) {
                cost += cost * 0.2;
            }

            return cost;

        } catch (Exception e) {
            log.error("Error calculating delivery cost", e);
            throw new DeliveryCalculationException("Error calculating delivery", e);
        }
    }

    public UUID planDelivery(DeliveryRequest request) {

        validateRequest(request);

        AddressDTO warehouseAddress = warehouseClient.getWarehouseAddress();

        if (warehouseAddress == null) {
            throw new DeliveryCalculationException("Warehouse address is null", null);
        }

        Double cost = calculateCost(request);

        String warehouseAddressString = warehouseAddress.getCountry() + ", " + warehouseAddress.getCity() + ", " + warehouseAddress.getStreet() + ", " + warehouseAddress.getHouse();

        Delivery delivery = Delivery.builder().orderId(request.getOrderId()).weight(request.getWeight()).volume(request.getVolume()).fragile(request.getFragile()).warehouseAddress(warehouseAddressString).deliveryAddress(addressMapper.toEntity(request.getDeliveryAddress())).status(DeliveryStatus.CREATED).deliveryCost(cost).build();

        delivery = repository.save(delivery);

        log.info("Delivery created: {}", delivery.getId());

        return delivery.getId();
    }

    @Transactional
    public void startDelivery(UUID deliveryId) {

        Delivery delivery = getDeliveryOrThrow(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.CREATED) {
            throw new IllegalStateException("Delivery already started or finished");
        }

        delivery.setStatus(DeliveryStatus.IN_PROGRESS);

        warehouseClient.shippedToDelivery(delivery.getOrderId(), deliveryId);
        orderClient.deliverySuccess(delivery.getOrderId());

        log.info("Delivery started: {}", deliveryId);
    }

    @Transactional
    public void completeDelivery(UUID deliveryId) {

        Delivery delivery = getDeliveryOrThrow(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            throw new IllegalStateException("Delivery is not in progress");
        }

        delivery.setStatus(DeliveryStatus.DELIVERED);

        orderClient.deliverySuccess(delivery.getOrderId());

        log.info("Delivery completed: {}", deliveryId);
    }

    @Transactional
    public void failDelivery(UUID deliveryId) {

        Delivery delivery = getDeliveryOrThrow(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
            throw new IllegalStateException("Delivery is not in progress");
        }

        delivery.setStatus(DeliveryStatus.FAILED);

        orderClient.deliveryFailed(delivery.getOrderId());

        log.info("Delivery failed: {}", deliveryId);
    }

    private void validateRequest(DeliveryRequest request) {
        if (request == null || request.getOrderId() == null || request.getWeight() == null || request.getVolume() == null || request.getDeliveryAddress() == null || request.getDeliveryAddress().getStreet() == null) {

            throw new DeliveryCalculationException("Invalid delivery request", null);
        }
    }

    private Delivery getDeliveryOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Delivery not found: " + id));
    }
}