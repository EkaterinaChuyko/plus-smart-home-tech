package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.WarehouseItem;

import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<WarehouseItem, Long> {
    Optional<WarehouseItem> findByProductId(Long productId);
}
