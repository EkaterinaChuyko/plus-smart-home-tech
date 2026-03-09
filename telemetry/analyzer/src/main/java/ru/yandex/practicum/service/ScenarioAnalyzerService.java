package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.enums.ConditionOperation;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
@Service
public class ScenarioAnalyzerService {

    public boolean checkScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        log.info("========== CHECKING SCENARIO '{}' ==========", scenario.getName());

        boolean result = true;
        for (ScenarioCondition scenarioCondition : scenario.getScenarioConditions()) {
            boolean conditionResult = checkCondition(scenarioCondition, snapshot);
            log.info("Condition for sensor {}: {}",
                    scenarioCondition.getSensor().getId(), conditionResult);

            if (!conditionResult) {
                result = false;
                break;
            }
        }

        log.info("Scenario '{}' final result: {}", scenario.getName(), result);
        return result;
    }

    private boolean checkCondition(ScenarioCondition scenarioCondition, SensorsSnapshotAvro snapshot) {
        String sensorId = scenarioCondition.getSensor().getId();
        Condition condition = scenarioCondition.getCondition();

        log.info("  Checking sensor {}: type={}, op={}, expected={}",
                sensorId, condition.getType(), condition.getOperation(), condition.getValue());

        SensorStateAvro sensorState = snapshot.getSensorsState().get(sensorId);
        if (sensorState == null) {
            log.warn("  Sensor {} not found in snapshot", sensorId);
            log.info("  Available sensors: {}", snapshot.getSensorsState().keySet());
            return false;
        }

        log.info("  Sensor data: {}", sensorState.getData());

        Object actualValue = extractValue(sensorState.getData(), condition.getType());
        log.info("  Extracted value: {} ({})", actualValue,
                actualValue != null ? actualValue.getClass().getSimpleName() : "null");

        if (actualValue == null) {
            return false;
        }

        boolean result = compare(actualValue, condition.getOperation(), condition.getValue());
        log.info("  Comparison result: {}", result);

        return result;
    }

    private Object extractValue(Object sensorData, ConditionType type) {
        if (sensorData == null) return null;

        log.debug("Extracting value from {} for type {}", sensorData.getClass().getSimpleName(), type);

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

        log.warn("Could not extract value from {} for type {}",
                sensorData.getClass().getSimpleName(), type);
        return null;
    }


    private boolean compare(Object actual, ConditionOperation op, int expected) {
        if (actual instanceof Boolean) {
            boolean boolVal = (Boolean) actual;
            boolean expectedBool = expected == 1;

            switch (op) {
                case EQUALS:
                    return boolVal == expectedBool;
                default:
                    return false;
            }
        }

        if (actual instanceof Integer) {
            int intVal = (Integer) actual;

            switch (op) {
                case EQUALS:
                    return intVal == expected;
                case GREATER_THAN:
                    return intVal > expected;
                case LOWER_THAN:
                    return intVal < expected;
                default:
                    return false;
            }
        }

        return false;
    }
}