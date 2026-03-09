package ru.yandex.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.config.KafkaConfig;
import ru.yandex.practicum.enums.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> hubConsumer;
    private final KafkaConfig kafkaConfig;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    public HubEventProcessor(KafkaConfig kafkaConfig,
                             SensorRepository sensorRepository,
                             ScenarioRepository scenarioRepository,
                             ConditionRepository conditionRepository,
                             ActionRepository actionRepository,
                             ScenarioConditionRepository scenarioConditionRepository,
                             ScenarioActionRepository scenarioActionRepository) {
        this.kafkaConfig = kafkaConfig;
        this.hubConsumer = kafkaConfig.createHubEventConsumer("hub-group");
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
        this.conditionRepository = conditionRepository;
        this.actionRepository = actionRepository;
        this.scenarioConditionRepository = scenarioConditionRepository;
        this.scenarioActionRepository = scenarioActionRepository;
    }

    @Override
    public void run() {
        log.info("HubEventProcessor started");
        log.info("Subscribing to topic: {}", kafkaConfig.getHubsTopic());

        try (hubConsumer) {
            Runtime.getRuntime().addShutdownHook(new Thread(hubConsumer::wakeup));
            hubConsumer.subscribe(List.of(kafkaConfig.getHubsTopic()));
            log.info("Successfully subscribed to topic: {}", kafkaConfig.getHubsTopic());

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = hubConsumer.poll(Duration.ofSeconds(5));

                if (records.isEmpty()) {
                    log.debug("No hub event records received");
                } else {
                    log.info("Received {} hub event records", records.count());

                    for (ConsumerRecord<String, HubEventAvro> record : records) {
                        log.info("Processing hub event from partition {}, offset {}",
                                record.partition(), record.offset());
                        HubEventAvro hubEventAvro = record.value();
                        processHubEvent(hubEventAvro);
                    }
                }

                hubConsumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Commit hubEvent processing error. Offsets: {}", offsets, exception);
                    }
                });
            }
        } catch (WakeupException ignored) {
            log.info("HubEventProcessor wakeup");
        } catch (Exception e) {
            log.error("Error handling HubEvents from kafka", e);
        } finally {
            hubConsumer.close();
            log.info("HubEventProcessor closed");
        }
    }

    @Transactional
    void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId().toString();
        Object payload = event.getPayload();

        log.info("Processing hub event for hub: {}, payload type: {}",
                hubId, payload.getClass().getSimpleName());

        if (payload instanceof DeviceAddedEventAvro deviceAddedEventAvro) {
            Sensor sensor = new Sensor();
            sensor.setId(deviceAddedEventAvro.getId().toString());
            sensor.setHubId(hubId);
            sensorRepository.save(sensor);
            log.info("Added sensor {} for hub {}", sensor.getId(), hubId);

        } else if (payload instanceof DeviceRemovedEventAvro deviceRemovedEventAvro) {
            sensorRepository.deleteById(deviceRemovedEventAvro.getId().toString());
            log.info("Removed sensor {} from hub {}", deviceRemovedEventAvro.getId(), hubId);

        } else if (payload instanceof ScenarioAddedEventAvro scenarioAddedEventAvro) {
            createNewScenario(hubId, scenarioAddedEventAvro);

        } else if (payload instanceof ScenarioRemovedEventAvro scenarioRemovedEventAvro) {
            String scenarioName = scenarioRemovedEventAvro.getName().toString();
            scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                    .ifPresent(scenario -> scenarioRepository.delete(scenario));
            log.info("Removed scenario {} from hub {}", scenarioName, hubId);

        } else {
            log.warn("Unknown payload type: {}", payload.getClass().getSimpleName());
        }
    }

    @Transactional
    protected void createNewScenario(String hubId, ScenarioAddedEventAvro event) {
        log.info("Creating new scenario: {}", event.getName());

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(event.getName().toString());
        final Scenario savedScenario = scenarioRepository.save(scenario);
        log.info("Created scenario with id: {}", savedScenario.getId());

        AtomicInteger conditionCount = new AtomicInteger(0);
        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            String sensorId = conditionAvro.getSensorId().toString();

            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Condition condition = new Condition();
                condition.setType(mapConditionType(conditionAvro.getType()));
                condition.setOperation(mapOperation(conditionAvro.getOperation()));
                condition.setValue(extractValue(conditionAvro.getValue()));
                Condition savedCondition = conditionRepository.save(condition);

                ScenarioConditionId id = new ScenarioConditionId();
                id.setScenarioId(savedScenario.getId());
                id.setSensorId(sensorId);
                id.setConditionId(savedCondition.getId());

                ScenarioCondition scenarioCondition = new ScenarioCondition();
                scenarioCondition.setId(id);
                scenarioCondition.setScenario(savedScenario);
                scenarioCondition.setSensor(sensor);
                scenarioCondition.setCondition(savedCondition);

                scenarioConditionRepository.save(scenarioCondition);

                log.info("Created condition {} for sensor {} and linked to scenario {}",
                        savedCondition.getId(), sensorId, savedScenario.getId());
                conditionCount.incrementAndGet();
            });
        }
        log.info("Processed {} conditions for scenario {}", conditionCount.get(), savedScenario.getName());

        AtomicInteger actionCount = new AtomicInteger(0);
        for (DeviceActionAvro actionAvro : event.getActions()) {
            String sensorId = actionAvro.getSensorId().toString();

            sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
                Action action = new Action();
                action.setType(mapActionType(actionAvro.getType()));
                action.setValue((Integer) actionAvro.getValue());
                Action savedAction = actionRepository.save(action);

                ScenarioActionId id = new ScenarioActionId();
                id.setScenarioId(savedScenario.getId());
                id.setSensorId(sensorId);
                id.setActionId(savedAction.getId());

                ScenarioAction scenarioAction = new ScenarioAction();
                scenarioAction.setId(id);
                scenarioAction.setScenario(savedScenario);
                scenarioAction.setSensor(sensor);
                scenarioAction.setAction(savedAction);

                scenarioActionRepository.save(scenarioAction);

                log.info("Created action {} for sensor {} and linked to scenario {}",
                        savedAction.getId(), sensorId, savedScenario.getId());
                actionCount.incrementAndGet();
            });
        }
        log.info("Processed {} actions for scenario {}", actionCount.get(), savedScenario.getName());
    }

    private ConditionType mapConditionType(ConditionTypeAvro type) {
        switch (type) {
            case MOTION: return ConditionType.MOTION;
            case LUMINOSITY: return ConditionType.LUMINOSITY;
            case SWITCH: return ConditionType.SWITCH;
            case TEMPERATURE: return ConditionType.TEMPERATURE;
            case CO2LEVEL: return ConditionType.CO2LEVEL;
            case HUMIDITY: return ConditionType.HUMIDITY;
            default: throw new IllegalArgumentException("Unknown condition type: " + type);
        }
    }

    private ConditionOperation mapOperation(ConditionOperationAvro op) {
        switch (op) {
            case EQUALS: return ConditionOperation.EQUALS;
            case GREATER_THAN: return ConditionOperation.GREATER_THAN;
            case LOWER_THAN: return ConditionOperation.LOWER_THAN;
            default: throw new IllegalArgumentException("Unknown operation: " + op);
        }
    }

    private ActionType mapActionType(ActionTypeAvro type) {
        switch (type) {
            case ACTIVATE: return ActionType.ACTIVATE;
            case DEACTIVATE: return ActionType.DEACTIVATE;
            case INVERSE: return ActionType.INVERSE;
            case SET_VALUE: return ActionType.SET_VALUE;
            default: throw new IllegalArgumentException("Unknown action type: " + type);
        }
    }

    private Integer extractValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        log.warn("Unknown value type: {}", value.getClass().getSimpleName());
        return null;
    }
}