package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckRequestDto;
import ru.yandex.practicum.dto.warehouse.WarehouseCheckResponseDto;

@FeignClient(name = "warehouse", path = "/warehouse")
public interface WarehouseClient {

    @PostMapping("/check")
    WarehouseCheckResponseDto checkAvailability(@RequestBody WarehouseCheckRequestDto request);
}
