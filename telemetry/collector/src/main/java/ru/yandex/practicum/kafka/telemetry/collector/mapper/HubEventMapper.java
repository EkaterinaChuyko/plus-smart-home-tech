package ru.yandex.practicum.kafka.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.condition.ConditionOperation;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.condition.ConditionType;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.device.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.scenario.ScenarioAddedEvent;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.scenario.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.scenario.ScenarioRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder().setHubId(event.getHubId()).setTimestamp(Instant.ofEpochMilli(event.getTimestamp().toEpochMilli()));

        if (event instanceof DeviceAddedEvent) {
            DeviceAddedEvent e = (DeviceAddedEvent) event;
            DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder().setId(e.getId()).setType(mapDeviceType(e.getDeviceType())).build();
            builder.setPayload(payload);

        } else if (event instanceof DeviceRemovedEvent) {
            DeviceRemovedEvent e = (DeviceRemovedEvent) event;
            DeviceRemovedEventAvro payload = DeviceRemovedEventAvro.newBuilder().setId(e.getId()).build();
            builder.setPayload(payload);

        } else if (event instanceof ScenarioAddedEvent) {
            ScenarioAddedEvent e = (ScenarioAddedEvent) event;
            ScenarioAddedEventAvro payload = ScenarioAddedEventAvro.newBuilder().setName(e.getName()).setConditions(mapConditions(e.getConditions())).setActions(mapActions(e.getActions())).build();
            builder.setPayload(payload);

        } else if (event instanceof ScenarioRemovedEvent) {
            ScenarioRemovedEvent e = (ScenarioRemovedEvent) event;
            ScenarioRemovedEventAvro payload = ScenarioRemovedEventAvro.newBuilder().setName(e.getName()).build();
            builder.setPayload(payload);
        }

        return builder.build();
    }

    private DeviceTypeAvro mapDeviceType(DeviceType type) {
        switch (type) {
            case MOTION_SENSOR:
                return DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR:
                return DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR:
                return DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR:
                return DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR:
                return DeviceTypeAvro.SWITCH_SENSOR;
            default:
                throw new IllegalArgumentException("Unknown device type: " + type);
        }
    }

    private List<ScenarioConditionAvro> mapConditions(List<ScenarioCondition> conditions) {
        return conditions.stream().map(this::mapCondition).toList();
    }

    private ScenarioConditionAvro mapCondition(ScenarioCondition c) {
        return ScenarioConditionAvro.newBuilder().setSensorId(c.getSensorId()).setType(mapConditionType(c.getType())).setOperation(mapOperation(c.getOperation())).setValue(c.getValue()).build();
    }

    private ConditionTypeAvro mapConditionType(ConditionType type) {
        switch (type) {
            case MOTION:
                return ConditionTypeAvro.MOTION;
            case LUMINOSITY:
                return ConditionTypeAvro.LUMINOSITY;
            case SWITCH:
                return ConditionTypeAvro.SWITCH;
            case TEMPERATURE:
                return ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL:
                return ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY:
                return ConditionTypeAvro.HUMIDITY;
            default:
                throw new IllegalArgumentException("Unknown condition type: " + type);
        }
    }

    private ConditionOperationAvro mapOperation(ConditionOperation op) {
        switch (op) {
            case EQUALS:
                return ConditionOperationAvro.EQUALS;
            case GREATER_THAN:
                return ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN:
                return ConditionOperationAvro.LOWER_THAN;
            default:
                throw new IllegalArgumentException("Unknown operation: " + op);
        }
    }

    private List<DeviceActionAvro> mapActions(List<DeviceAction> actions) {
        return actions.stream().map(this::mapAction).toList();
    }

    private DeviceActionAvro mapAction(DeviceAction a) {
        return DeviceActionAvro.newBuilder().setSensorId(a.getSensorId()).setType(mapActionType(a.getType())).setValue(a.getValue()).build();
    }

    private ActionTypeAvro mapActionType(ActionType type) {
        switch (type) {
            case ACTIVATE:
                return ActionTypeAvro.ACTIVATE;
            case DEACTIVATE:
                return ActionTypeAvro.DEACTIVATE;
            case INVERSE:
                return ActionTypeAvro.INVERSE;
            case SET_VALUE:
                return ActionTypeAvro.SET_VALUE;
            default:
                throw new IllegalArgumentException("Unknown action type: " + type);
        }
    }
}





















