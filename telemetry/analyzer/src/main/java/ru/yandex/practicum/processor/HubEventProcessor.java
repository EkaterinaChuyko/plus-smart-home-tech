package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.Sensor;
import ru.yandex.practicum.enums.ActionType;
import ru.yandex.practicum.enums.ConditionOperation;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;


import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Value("${spring.kafka.topics.hubs}")
    private String hubsTopic;

    @Override
    public void run() {
        consumer.subscribe(List.of(hubsTopic));
        log.info("HubEventProcessor subscribed to topic: {}", hubsTopic);

        try {
            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    processHubEvent(record.value());
                }

                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor received shutdown signal");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            consumer.close();
            log.info("HubEventProcessor closed");
        }
    }

    @Transactional
    void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId().toString();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro) {
            processDeviceAdded(hubId, (DeviceAddedEventAvro) payload);
        } else if (payload instanceof DeviceRemovedEventAvro) {
            processDeviceRemoved(hubId, (DeviceRemovedEventAvro) payload);
        } else if (payload instanceof ScenarioAddedEventAvro) {
            processScenarioAdded(hubId, (ScenarioAddedEventAvro) payload);
        } else if (payload instanceof ScenarioRemovedEventAvro) {
            processScenarioRemoved(hubId, (ScenarioRemovedEventAvro) payload);
        }
    }

    private void processDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId().toString();

        if (!sensorRepository.existsById(sensorId)) {
            Sensor sensor = new Sensor();
            sensor.setId(sensorId);
            sensor.setHubId(hubId);
            sensorRepository.save(sensor);
            log.info("Added sensor {} for hub {}", sensorId, hubId);
        }
    }

    private void processDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId().toString();

        sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
            sensorRepository.delete(sensor);
            log.info("Removed sensor {} from hub {}", sensorId, hubId);
        });
    }

    private void processScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String scenarioName = event.getName().toString();

        if (scenarioRepository.findByHubIdAndName(hubId, scenarioName).isEmpty()) {
            createNewScenario(hubId, event);
            log.info("Added scenario {} for hub {}", scenarioName, hubId);
        }
    }

    private void createNewScenario(String hubId, ScenarioAddedEventAvro event) {
        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(event.getName().toString());
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            String sensorId = conditionAvro.getSensorId().toString();

            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Condition condition = new Condition();
                condition.setType(mapConditionType(conditionAvro.getType()));
                condition.setOperation(mapOperation(conditionAvro.getOperation()));
                condition.setValue(extractValue(conditionAvro.getValue()));
                conditionRepository.save(condition);
            });
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            String sensorId = actionAvro.getSensorId().toString();

            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Action action = new Action();
                action.setType(mapActionType(actionAvro.getType()));
                action.setValue((Integer) actionAvro.getValue());
                actionRepository.save(action);
            });
        }
    }

    private void processScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName().toString();

        scenarioRepository.findByHubIdAndName(hubId, scenarioName).ifPresent(scenario -> {
            scenarioRepository.delete(scenario);
            log.info("Removed scenario {} from hub {}", scenarioName, hubId);
        });
    }

    private ConditionType mapConditionType(ConditionTypeAvro type) {
        return switch (type) {
            case MOTION -> ConditionType.MOTION;
            case LUMINOSITY -> ConditionType.LUMINOSITY;
            case SWITCH -> ConditionType.SWITCH;
            case TEMPERATURE -> ConditionType.TEMPERATURE;
            case CO2LEVEL -> ConditionType.CO2LEVEL;
            case HUMIDITY -> ConditionType.HUMIDITY;
        };
    }

    private ConditionOperation mapOperation(ConditionOperationAvro op) {
        return switch (op) {
            case EQUALS -> ConditionOperation.EQUALS;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
        };
    }

    private ActionType mapActionType(ActionTypeAvro type) {
        return switch (type) {
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case INVERSE -> ActionType.INVERSE;
            case SET_VALUE -> ActionType.SET_VALUE;
        };
    }

    private Integer extractValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        return null;
    }
}