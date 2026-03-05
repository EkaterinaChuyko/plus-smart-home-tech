package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scenario_actions")
@Getter
@Setter
public class ScenarioAction {

    @EmbeddedId
    private ScenarioActionId id;

    @ManyToOne
    @MapsId("scenarioId")
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @ManyToOne
    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id", insertable = false, updatable = false)
    private Sensor sensor;

    @ManyToOne
    @JoinColumn(name = "action_id", insertable = false, updatable = false)
    private Action action;
}