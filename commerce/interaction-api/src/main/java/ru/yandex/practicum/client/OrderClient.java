package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.UUID;

@FeignClient(name = "order", path = "/order")
public interface OrderClient {

    @PostMapping("/{id}/payment/success")
    void paymentSuccess(@PathVariable UUID id);

    @PostMapping("/{id}/payment/failed")
    void paymentFailed(@PathVariable UUID id);

    @PostMapping("/{id}/delivery/success")
    void deliverySuccess(@PathVariable UUID id);

    @PostMapping("/{id}/delivery/failed")
    void deliveryFailed(@PathVariable UUID id);
}