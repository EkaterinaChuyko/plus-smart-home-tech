package ru.yandex.practicum.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TotalCostRequest {

    private BigDecimal productsCost;
    private BigDecimal deliveryCost;
}