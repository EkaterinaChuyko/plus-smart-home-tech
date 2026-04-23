package ru.yandex.practicum.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.address.AddressDTO;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {
    private UUID cartId;
    private List<OrderItemDto> items;
    private Double weight;
    private Double volume;
    private Boolean fragile;
    private AddressDTO deliveryAddress;
}
