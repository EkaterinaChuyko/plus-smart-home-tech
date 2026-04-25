package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.address.AddressEmbeddable;
import ru.yandex.practicum.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID cartId;

    private UUID paymentId;

    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal productsPrice;
    private BigDecimal deliveryPrice;
    private BigDecimal totalPrice;

    private Double weight;
    private Double volume;

    private Boolean fragile;

    @Embedded
    private AddressEmbeddable deliveryAddress;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

}
