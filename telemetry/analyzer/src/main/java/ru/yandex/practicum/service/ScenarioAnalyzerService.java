package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
@Service
public class ScenarioAnalyzerService {

    public boolean checkScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        log.debug("Checking scenario '{}' with {} conditions",
                scenario.getName(),
                scenario.getScenarioConditions().size());

        boolean result = scenario.getScenarioConditions().stream()
                .allMatch(condition -> {
                    boolean conditionResult = checkCondition(condition, snapshot);
                    log.debug("Condition for sensor {}: {}", condition.getSensor().getId(), conditionResult);
                    return conditionResult;
                });

        log.debug("Scenario '{}' result: {}", scenario.getName(), result);
        return result;
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, SensorsSnapshotAvro snapshot) {
        if (scenarioCondition.getSensor() == null) {
            log.error("Sensor is null in scenario condition");
            return false;
        }

        String sensorId = scenarioCondition.getSensor().getId();
        Condition condition = scenarioCondition.getCondition();

        if (condition == null) {
            log.error("Condition is null for sensor {}", sensorId);
            return false;
        }

        log.info("========== CHECKING CONDITION FOR SENSOR {} ==========", sensorId);
        log.info("Condition type: {}, operation: {}, expected value: {}",
                condition.getType(), condition.getOperation(), condition.getValue());

        SensorStateAvro sensorState = snapshot.getSensorsState().get(sensorId);
        if (sensorState == null) {
            log.warn("Sensor {} not found in snapshot", sensorId);
            log.info("Available sensors in snapshot: {}", snapshot.getSensorsState().keySet());
            return false;
        }

        log.info("Sensor state timestamp: {}", sensorState.getTimestamp());
        log.info("Sensor data class: {}", sensorState.getData().getClass().getSimpleName());
        log.info("Sensor data: {}", sensorState.getData());

        Object actualValue = extractValue(sensorState.getData(), condition.getType());
        log.info("Extracted value: {} (class: {})", actualValue, actualValue != null ? actualValue.getClass().getSimpleName() : "null");

        if (actualValue == null) {
            log.warn("Could not extract value for sensor {} of type {}", sensorId, condition.getType());
            return false;
        }

        int expectedValue = condition.getValue();
        boolean result;

        switch (condition.getOperation()) {
            case EQUALS:
                result = compareEquals(actualValue, expectedValue);
                log.info("COMPARE: actual={} == expected={} ? {}", actualValue, expectedValue, result);
                break;
            case GREATER_THAN:
                result = compareGreaterThan(actualValue, expectedValue);
                log.info("COMPARE: actual={} > expected={} ? {}", actualValue, expectedValue, result);
                break;
            case LOWER_THAN:
                result = compareLessThan(actualValue, expectedValue);
                log.info("COMPARE: actual={} < expected={} ? {}", actualValue, expectedValue, result);
                break;
            default:
                log.warn("Unknown operation: {}", condition.getOperation());
                result = false;
        }

        log.info("Condition result for sensor {}: {}", sensorId, result);
        return result;
    }

    private Object extractValue(Object sensorData, ConditionType type) {
        if (sensorData == null) return null;

        switch (type) {
            case MOTION:
                if (sensorData instanceof MotionSensorAvro) {
                    return ((MotionSensorAvro) sensorData).getMotion();
                }
                break;
            case LUMINOSITY:
                if (sensorData instanceof LightSensorAvro) {
                    return ((LightSensorAvro) sensorData).getLuminosity();
                }
                break;
            case SWITCH:
                if (sensorData instanceof SwitchSensorAvro) {
                    return ((SwitchSensorAvro) sensorData).getState();
                }
                break;
            case TEMPERATURE:
                if (sensorData instanceof TemperatureSensorAvro) {
                    return ((TemperatureSensorAvro) sensorData).getTemperatureC();
                }
                if (sensorData instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) sensorData).getTemperatureC();
                }
                break;
            case CO2LEVEL:
                if (sensorData instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) sensorData).getCo2Level();
                }
                break;
            case HUMIDITY:
                if (sensorData instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) sensorData).getHumidity();
                }
                break;
        }
        log.warn("Could not extract value from {} for type {}", sensorData.getClass().getSimpleName(), type);
        return null;
    }

    private boolean compareEquals(Object actual, int expected) {
        log.debug("compareEquals: actual={} ({}), expected={}",
                actual, actual != null ? actual.getClass().getSimpleName() : "null", expected);

        if (actual instanceof Boolean) {
            boolean boolValue = (Boolean) actual;
            boolean expectedBool = expected == 1;
            log.debug("Boolean comparison: {} == {} ? {}", boolValue, expectedBool, boolValue == expectedBool);
            return boolValue == expectedBool;
        }
        if (actual instanceof Integer) {
            int intValue = (Integer) actual;
            log.debug("Integer comparison: {} == {} ? {}", intValue, expected, intValue == expected);
            return intValue == expected;
        }
        log.debug("Unsupported type for equals comparison: {}", actual.getClass().getSimpleName());
        return false;
    }

    private boolean compareGreaterThan(Object actual, int expected) {
        log.debug("compareGreaterThan: actual={}, expected={}", actual, expected);

        if (actual instanceof Integer) {
            int intValue = (Integer) actual;
            log.debug("Integer comparison: {} > {} ? {}", intValue, expected, intValue > expected);
            return intValue > expected;
        }
        log.debug("Unsupported type for greater than comparison: {}", actual.getClass().getSimpleName());
        return false;
    }

    private boolean compareLessThan(Object actual, int expected) {
        log.debug("compareLessThan: actual={}, expected={}", actual, expected);

        if (actual instanceof Integer) {
            int intValue = (Integer) actual;
            log.debug("Integer comparison: {} < {} ? {}", intValue, expected, intValue < expected);
            return intValue < expected;
        }
        log.debug("Unsupported type for less than comparison: {}", actual.getClass().getSimpleName());
        return false;
    }
}