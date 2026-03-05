package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Service
public class ScenarioAnalyzerService {

    public boolean checkScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        return scenario.getScenarioConditions().stream().allMatch(sc -> checkCondition(sc, snapshot));
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, SensorsSnapshotAvro snapshot) {
        String sensorId = scenarioCondition.getSensor().getId();
        Condition condition = scenarioCondition.getCondition();

        SensorStateAvro sensorState = snapshot.getSensorsState().get(sensorId);
        if (sensorState == null) return false;

        Object actualValue = extractValue(sensorState.getData(), condition.getType());
        if (actualValue == null) return false;

        int expectedValue = condition.getValue();

        return switch (condition.getOperation()) {
            case EQUALS -> compareEquals(actualValue, expectedValue);
            case GREATER_THAN -> compareGreaterThan(actualValue, expectedValue);
            case LOWER_THAN -> compareLessThan(actualValue, expectedValue);
        };
    }

    private Object extractValue(Object sensorData, ConditionType type) {
        return switch (type) {
            case MOTION -> sensorData instanceof MotionSensorAvro ? ((MotionSensorAvro) sensorData).getMotion() : null;
            case LUMINOSITY ->
                    sensorData instanceof LightSensorAvro ? ((LightSensorAvro) sensorData).getLuminosity() : null;
            case SWITCH -> sensorData instanceof SwitchSensorAvro ? ((SwitchSensorAvro) sensorData).getState() : null;
            case TEMPERATURE ->
                    sensorData instanceof TemperatureSensorAvro ? ((TemperatureSensorAvro) sensorData).getTemperatureC() : sensorData instanceof ClimateSensorAvro ? ((ClimateSensorAvro) sensorData).getTemperatureC() : null;
            case CO2LEVEL ->
                    sensorData instanceof ClimateSensorAvro ? ((ClimateSensorAvro) sensorData).getCo2Level() : null;
            case HUMIDITY ->
                    sensorData instanceof ClimateSensorAvro ? ((ClimateSensorAvro) sensorData).getHumidity() : null;
        };
    }

    private boolean compareEquals(Object actual, int expected) {
        if (actual instanceof Boolean) return ((Boolean) actual) == (expected == 1);
        if (actual instanceof Integer) return ((Integer) actual) == expected;
        return false;
    }

    private boolean compareGreaterThan(Object actual, int expected) {
        return actual instanceof Integer && ((Integer) actual) > expected;
    }

    private boolean compareLessThan(Object actual, int expected) {
        return actual instanceof Integer && ((Integer) actual) < expected;
    }
}
