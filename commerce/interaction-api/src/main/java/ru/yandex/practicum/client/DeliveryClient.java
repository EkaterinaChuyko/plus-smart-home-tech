package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.delivery.DeliveryRequest;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "delivery", path = "/delivery")
public interface DeliveryClient {

    @PostMapping
    UUID planDelivery(@RequestBody DeliveryRequest request);

    @PostMapping("/cost")
    BigDecimal calculateCost(@RequestBody DeliveryRequest request);

    @PostMapping("/{id}/start")
    void start(@PathVariable UUID id);

    @PostMapping("/{id}/success")
    void success(@PathVariable UUID id);

    @PostMapping("/{id}/fail")
    void fail(@PathVariable UUID id);
}
