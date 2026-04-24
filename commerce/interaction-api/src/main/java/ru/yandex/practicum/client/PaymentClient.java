package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "payment", path = "/payment")
public interface PaymentClient {

    @PostMapping("/products-cost")
    BigDecimal productCost(@RequestBody OrderRequest request);

    @PostMapping("/total")
    BigDecimal getTotalCost(@RequestBody TotalCostRequest request);

    @PostMapping("/pay")
    UUID payment(@RequestBody PaymentRequest request);

    @PostMapping("/{id}/success")
    void success(@PathVariable UUID id);

    @PostMapping("/{id}/failed")
    void failed(@PathVariable UUID id);
}