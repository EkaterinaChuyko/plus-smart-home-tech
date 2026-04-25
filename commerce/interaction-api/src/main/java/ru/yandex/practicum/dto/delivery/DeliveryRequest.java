package ru.yandex.practicum.dto.delivery;

import lombok.*;
import ru.yandex.practicum.address.AddressDTO;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryRequest {

    private UUID orderId;
    private Double weight;
    private Double volume;
    private Boolean fragile;

    private String warehouseAddress;
    private AddressDTO deliveryAddress;
}