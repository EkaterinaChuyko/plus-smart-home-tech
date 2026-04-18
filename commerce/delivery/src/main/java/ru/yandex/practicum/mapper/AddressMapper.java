package ru.yandex.practicum.mapper;


import org.springframework.stereotype.Component;
import ru.yandex.practicum.address.Address;
import ru.yandex.practicum.address.AddressDTO;

@Component
public class AddressMapper {

    public Address toEntity(AddressDTO dto) {
        if (dto == null) return null;

        return new Address(dto.getCountry(), dto.getCity(), dto.getStreet(), dto.getHouse(), dto.getFlat());
    }

    public AddressDTO toDto(Address entity) {
        if (entity == null) return null;

        return new AddressDTO(entity.getCountry(), entity.getCity(), entity.getStreet(), entity.getHouse(), entity.getFlat());
    }
}
