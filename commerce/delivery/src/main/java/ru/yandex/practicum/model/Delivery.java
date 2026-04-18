package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.address.Address;
import ru.yandex.practicum.enums.DeliveryStatus;

import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;

    private Double weight;
    private Double volume;
    private Boolean fragile;

    private String warehouseAddress;

    @Column(name = "delivery_cost")
    private Double deliveryCost;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "country", column = @Column(name = "delivery_country")), @AttributeOverride(name = "city", column = @Column(name = "delivery_city")), @AttributeOverride(name = "street", column = @Column(name = "delivery_street")), @AttributeOverride(name = "house", column = @Column(name = "delivery_house")), @AttributeOverride(name = "flat", column = @Column(name = "delivery_flat"))})
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
}
