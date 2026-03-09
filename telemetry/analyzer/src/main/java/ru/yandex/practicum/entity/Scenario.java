package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hubId;
    private String name;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ScenarioCondition> scenarioConditions;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ScenarioAction> scenarioActions;
}
