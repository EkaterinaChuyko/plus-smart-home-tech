package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;

import java.util.UUID;

@FeignClient(name = "payment", path = "/payment")
public interface PaymentClient {

    @PostMapping("/products-cost")
    Double productCost(@RequestBody OrderRequest request);

    @PostMapping("/total")
    Double getTotalCost(@RequestBody TotalCostRequest request);

    @PostMapping("/pay")
    UUID payment(@RequestBody PaymentRequest request);
}
