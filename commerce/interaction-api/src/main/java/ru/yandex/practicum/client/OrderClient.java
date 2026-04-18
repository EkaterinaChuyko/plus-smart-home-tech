package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    @PostMapping("/{id}/payment/success")
    void paymentSuccess(@PathVariable("id") UUID orderId);

    @PostMapping("/{id}/payment/failed")
    void paymentFailed(@PathVariable("id") UUID orderId);

    @PostMapping("/{id}/delivery/success")
    void deliverySuccess(@PathVariable("id") UUID orderId);

    @PostMapping("/{id}/delivery/failed")
    void deliveryFailed(@PathVariable("id") UUID orderId);
}