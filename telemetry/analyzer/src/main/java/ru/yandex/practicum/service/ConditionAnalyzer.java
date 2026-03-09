package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
@Service
public class ConditionAnalyzer {

    public boolean checkScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        return scenario.getScenarioConditions().stream().allMatch(condition -> checkCondition(condition, snapshot));
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, SensorsSnapshotAvro snapshot) {
        SensorStateAvro sensorState = snapshot.getSensorsState().get(scenarioCondition.getSensor().getId());

        if (sensorState == null) {
            log.debug("Sensor {} not found in snapshot", scenarioCondition.getSensor().getId());
            return false;
        }

        Object actualValue = extractValue(sensorState.getData(), scenarioCondition.getCondition().getType());
        int expectedValue = scenarioCondition.getCondition().getValue();

        boolean result = switch (scenarioCondition.getCondition().getOperation()) {
            case EQUALS -> compareEquals(actualValue, expectedValue);
            case GREATER_THAN -> compareGreaterThan(actualValue, expectedValue);
            case LOWER_THAN -> compareLessThan(actualValue, expectedValue);
        };

        log.debug("Condition result: sensor={}, actual={}, expected={}, op={}, result={}", scenarioCondition.getSensor().getId(), actualValue, expectedValue, scenarioCondition.getCondition().getOperation(), result);

        return result;
    }

    private Object extractValue(Object sensorData, ConditionType type) {
        return switch (type) {
            case MOTION -> sensorData instanceof MotionSensorAvro ? ((MotionSensorAvro) sensorData).getMotion() : null;
            case LUMINOSITY ->
                    sensorData instanceof LightSensorAvro ? ((LightSensorAvro) sensorData).getLuminosity() : null;
            case SWITCH -> sensorData instanceof SwitchSensorAvro ? ((SwitchSensorAvro) sensorData).getState() : null;
            case TEMPERATURE -> {
                if (sensorData instanceof TemperatureSensorAvro) {
                    yield ((TemperatureSensorAvro) sensorData).getTemperatureC();
                } else if (sensorData instanceof ClimateSensorAvro) {
                    yield ((ClimateSensorAvro) sensorData).getTemperatureC();
                }
                yield null;
            }
            case CO2LEVEL ->
                    sensorData instanceof ClimateSensorAvro ? ((ClimateSensorAvro) sensorData).getCo2Level() : null;
            case HUMIDITY ->
                    sensorData instanceof ClimateSensorAvro ? ((ClimateSensorAvro) sensorData).getHumidity() : null;
        };
    }

    private boolean compareEquals(Object actual, int expected) {
        if (actual instanceof Boolean) return (Boolean) actual == (expected == 1);
        if (actual instanceof Integer) return (Integer) actual == expected;
        return false;
    }

    private boolean compareGreaterThan(Object actual, int expected) {
        return actual instanceof Integer && (Integer) actual > expected;
    }

    private boolean compareLessThan(Object actual, int expected) {
        return actual instanceof Integer && (Integer) actual < expected;
    }
}
