package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.dto.order.OrderItemDto;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "warehouse", path = "/warehouse")
public interface WarehouseClient {

    @PostMapping("/assemble")
    void assemble(@RequestParam("orderId") UUID orderId,
                  @RequestBody List<OrderItemDto> items);

    @PostMapping("/shipped")
    void shippedToDelivery(@RequestParam("orderId") UUID orderId,
                           @RequestParam("deliveryId") UUID deliveryId);

    @GetMapping("/address")
    AddressDTO getWarehouseAddress();
}