package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import ru.yandex.practicum.enums.ConditionOperation;
import ru.yandex.practicum.enums.ConditionType;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "conditions")
@Getter
@Setter
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionOperation operation;

    private Integer value;
}