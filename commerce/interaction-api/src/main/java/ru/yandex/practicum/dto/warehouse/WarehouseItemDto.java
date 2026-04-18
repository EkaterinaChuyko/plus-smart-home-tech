package ru.yandex.practicum.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseItemDto {

    private Long productId;
    private Integer quantity;

    private Double weight;
    private Double width;
    private Double height;
    private Double depth;
    private Double volume;

    private Boolean fragile;
}