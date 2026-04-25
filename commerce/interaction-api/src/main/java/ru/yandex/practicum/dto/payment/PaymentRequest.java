package ru.yandex.practicum.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    private UUID orderId;
    private BigDecimal productsPrice;
    private BigDecimal deliveryPrice;
    private BigDecimal totalPrice;
}