package ru.yandex.practicum.kafka.telemetry.collector.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.collector.service.KafkaEventService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaEventService kafkaEventService;

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("=== Received HubEvent from hub: {} ===", request.getHubId());
        log.info("Event type: {}", request.getPayloadCase());

        try {
            HubEventAvro avroEvent = convertHubEvent(request);
            kafkaEventService.sendHubEvent(avroEvent);

            log.info("HubEvent successfully sent to Kafka");
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing HubEvent", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("=== Received SensorEvent from sensor: {} ===", request.getId());
        log.info("Hub ID: {}, event type: {}", request.getHubId(), request.getPayloadCase());

        try {
            SensorEventAvro avroEvent = convertSensorEvent(request);
            kafkaEventService.sendSensorEvent(avroEvent);

            log.info("SensorEvent successfully sent to Kafka");
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing SensorEvent", e);
            responseObserver.onError(e);
        }
    }

    private HubEventAvro convertHubEvent(HubEventProto proto) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder().setHubId(proto.getHubId()).setTimestamp(convertTimestampToInstant(proto.getTimestamp()));

        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                DeviceAddedEventProto deviceAdded = proto.getDeviceAdded();
                DeviceAddedEventAvro deviceAddedAvro = DeviceAddedEventAvro.newBuilder().setId(deviceAdded.getId()).setType(convertDeviceType(deviceAdded.getType())).build();
                builder.setPayload(deviceAddedAvro);
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();
                DeviceRemovedEventAvro deviceRemovedAvro = DeviceRemovedEventAvro.newBuilder().setId(deviceRemoved.getId()).build();
                builder.setPayload(deviceRemovedAvro);
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();

                List<ScenarioConditionAvro> conditions = new ArrayList<>();
                for (ScenarioConditionProto condition : scenarioAdded.getConditionsList()) {
                    conditions.add(convertCondition(condition));
                }

                List<DeviceActionAvro> actions = new ArrayList<>();
                for (DeviceActionProto action : scenarioAdded.getActionsList()) {
                    actions.add(convertAction(action));
                }

                ScenarioAddedEventAvro scenarioAddedAvro = ScenarioAddedEventAvro.newBuilder().setName(scenarioAdded.getName()).setConditions(conditions).setActions(actions).build();

                builder.setPayload(scenarioAddedAvro);
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();
                ScenarioRemovedEventAvro scenarioRemovedAvro = ScenarioRemovedEventAvro.newBuilder().setName(scenarioRemoved.getName()).build();
                builder.setPayload(scenarioRemovedAvro);
                break;

            default:
                log.warn("Unknown hub event payload type: {}", proto.getPayloadCase());
        }

        return builder.build();
    }

    private SensorEventAvro convertSensorEvent(SensorEventProto proto) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder().setId(proto.getId()).setHubId(proto.getHubId()).setTimestamp(convertTimestampToInstant(proto.getTimestamp()));

        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                MotionSensorProto motion = proto.getMotionSensor();
                MotionSensorAvro motionAvro = MotionSensorAvro.newBuilder().setLinkQuality(motion.getLinkQuality()).setMotion(motion.getMotion()).setVoltage(motion.getVoltage()).build();
                builder.setPayload(motionAvro);
                break;

            case TEMPERATURE_SENSOR:
                TemperatureSensorProto temp = proto.getTemperatureSensor();
                TemperatureSensorAvro tempAvro = TemperatureSensorAvro.newBuilder().setTemperatureC(temp.getTemperatureC()).setTemperatureF(temp.getTemperatureF()).build();
                builder.setPayload(tempAvro);
                break;

            case LIGHT_SENSOR:
                LightSensorProto light = proto.getLightSensor();
                LightSensorAvro lightAvro = LightSensorAvro.newBuilder().setLinkQuality(light.getLinkQuality()).setLuminosity(light.getLuminosity()).build();
                builder.setPayload(lightAvro);
                break;

            case CLIMATE_SENSOR:
                ClimateSensorProto climate = proto.getClimateSensor();
                ClimateSensorAvro climateAvro = ClimateSensorAvro.newBuilder().setTemperatureC(climate.getTemperatureC()).setHumidity(climate.getHumidity()).setCo2Level(climate.getCo2Level()).build();
                builder.setPayload(climateAvro);
                break;

            case SWITCH_SENSOR:
                SwitchSensorProto switchSensor = proto.getSwitchSensor();
                SwitchSensorAvro switchAvro = SwitchSensorAvro.newBuilder().setState(switchSensor.getState()).build();
                builder.setPayload(switchAvro);
                break;

            default:
                log.warn("Unknown sensor event payload type: {}", proto.getPayloadCase());
        }

        return builder.build();
    }

    private Instant convertTimestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private DeviceTypeAvro convertDeviceType(DeviceTypeProto type) {
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

    private ConditionTypeAvro convertConditionType(ConditionTypeProto type) {
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

    private ConditionOperationAvro convertOperation(ConditionOperationProto op) {
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

    private ActionTypeAvro convertActionType(ActionTypeProto type) {
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

    private ScenarioConditionAvro convertCondition(ScenarioConditionProto proto) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder().setSensorId(proto.getSensorId()).setType(convertConditionType(proto.getType())).setOperation(convertOperation(proto.getOperation()));

        switch (proto.getValueCase()) {
            case BOOL_VALUE:
                builder.setValue(proto.getBoolValue());
                break;
            case INT_VALUE:
                builder.setValue(proto.getIntValue());
                break;
            default:
                builder.setValue(null);
        }

        return builder.build();
    }

    private DeviceActionAvro convertAction(DeviceActionProto proto) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder().setSensorId(proto.getSensorId()).setType(convertActionType(proto.getType()));

        if (proto.hasValue()) {
            builder.setValue(proto.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }
}