package ru.yandex.practicum.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseAddressDto {

    private String country;
    private String city;
    private String street;
    private String building;
    private String apartment;
}
