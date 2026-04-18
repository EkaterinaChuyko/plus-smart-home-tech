package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.delivery.DeliveryRequest;

import java.util.UUID;

@FeignClient(name = "delivery")
public interface DeliveryClient {

    @PostMapping("/plan")
    UUID planDelivery(@RequestBody DeliveryRequest request);

    @PostMapping("/cost")
    Double deliveryCost(@RequestBody DeliveryRequest request);

    @PostMapping("/cost")
    Double calculateCost(@RequestBody DeliveryRequest request);
}
