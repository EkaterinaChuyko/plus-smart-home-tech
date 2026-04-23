package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.address.AddressDTO;
import ru.yandex.practicum.address.AddressEmbeddable;

@Component
public class OrderAddressMapper {

    public static AddressDTO toDto(AddressEmbeddable address) {
        if (address == null) return null;

        return new AddressDTO(address.getCountry(), address.getCity(), address.getStreet(), address.getHouse(), address.getFlat());
    }

    public static AddressEmbeddable toEmbeddable(AddressDTO dto) {
        if (dto == null) return null;

        return new AddressEmbeddable(dto.getCountry(), dto.getCity(), dto.getStreet(), dto.getHouse(), dto.getFlat());
    }

}
