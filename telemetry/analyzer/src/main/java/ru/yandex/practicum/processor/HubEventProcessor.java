package ru.yandex.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.config.KafkaConfig;
import ru.yandex.practicum.entity.*;
import ru.yandex.practicum.enums.ActionType;
import ru.yandex.practicum.enums.ConditionOperation;
import ru.yandex.practicum.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.repository.*;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final KafkaConfig kafkaConfig;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final String groupId;
    private KafkaConsumer<String, HubEventAvro> consumer;

    @Value("${spring.kafka.topics.hubs}")
    private String hubsTopic;

    public HubEventProcessor(KafkaConfig kafkaConfig, SensorRepository sensorRepository, ScenarioRepository scenarioRepository, ConditionRepository conditionRepository, ActionRepository actionRepository, ScenarioConditionRepository scenarioConditionRepository, ScenarioActionRepository scenarioActionRepository, @Value("${spring.kafka.consumer.hub.group-id}") String groupId) {
        this.kafkaConfig = kafkaConfig;
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
        this.conditionRepository = conditionRepository;
        this.actionRepository = actionRepository;
        this.scenarioConditionRepository = scenarioConditionRepository;
        this.scenarioActionRepository = scenarioActionRepository;
        this.groupId = groupId;
    }

    @PostConstruct
    public void start() {
        log.info("=== HubEventProcessor INIT ===");

        consumer = kafkaConfig.createHubEventConsumer(groupId);

        new Thread(this, "HubEventHandlerThread").start();
    }

    @Override
    public void run() {
        log.info("=== HubEventProcessor START ===");

        try (KafkaConsumer<String, HubEventAvro> consumer = kafkaConfig.createHubEventConsumer(groupId)) {
            consumer.subscribe(List.of(hubsTopic));

            while (!Thread.currentThread().isInterrupted()) {
                var records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    processHubEvent(record.value());
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor wakeup");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        }

        log.info("HubEventProcessor closed");
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
        Scenario savedScenario = new Scenario();
        savedScenario.setHubId(hubId);
        savedScenario.setName(event.getName().toString());
        scenarioRepository.save(savedScenario);

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            String sensorId = conditionAvro.getSensorId().toString();
            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Condition condition = new Condition();
                condition.setType(mapConditionType(conditionAvro.getType()));
                condition.setOperation(mapOperation(conditionAvro.getOperation()));
                condition.setValue(extractValue(conditionAvro.getValue()));
                condition = conditionRepository.save(condition);

                ScenarioCondition sc = new ScenarioCondition();
                ScenarioConditionId scId = new ScenarioConditionId();
                scId.setScenarioId(savedScenario.getId());
                scId.setSensorId(sensor.getId());
                scId.setConditionId(condition.getId());
                sc.setId(scId);
                sc.setScenario(savedScenario);
                sc.setSensor(sensor);
                sc.setCondition(condition);

                scenarioConditionRepository.save(sc);
            });
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            String sensorId = actionAvro.getSensorId().toString();
            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Action action = new Action();
                action.setType(mapActionType(actionAvro.getType()));
                action.setValue((Integer) actionAvro.getValue());
                action = actionRepository.save(action);

                ScenarioAction sa = new ScenarioAction();
                ScenarioActionId saId = new ScenarioActionId();
                saId.setScenarioId(savedScenario.getId());
                saId.setSensorId(sensor.getId());
                saId.setActionId(action.getId());
                sa.setId(saId);
                sa.setScenario(savedScenario);
                sa.setSensor(sensor);
                sa.setAction(action);

                scenarioActionRepository.save(sa);
            });
        }
    }

    private void processScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName().toString();
        scenarioRepository.findByHubIdAndName(hubId, scenarioName).ifPresent(scenario -> {
            scenarioConditionRepository.deleteAllByScenarioId(scenario.getId());
            scenarioActionRepository.deleteAllByScenarioId(scenario.getId());
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