package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;

    private BigDecimal productsPrice;
    private BigDecimal deliveryPrice;
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
