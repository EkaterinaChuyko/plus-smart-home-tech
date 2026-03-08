package ru.yandex.practicum.kafka.telemetry.collector.controller;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import telemetry.service.collector.CollectorGrpc;
import telemetry.service.collector.CollectorResponse;
import telemetry.service.collector.HubEvent;

import java.time.Instant;

@GrpcService
public class CollectorController extends CollectorGrpc.CollectorImplBase {

    private final KafkaTemplate<String, HubEventAvro> hubKafkaTemplate;
    private final KafkaTemplate<String, SensorEventAvro> sensorKafkaTemplate;

    public CollectorController(KafkaTemplate<String, HubEventAvro> hubKafkaTemplate, KafkaTemplate<String, SensorEventAvro> sensorKafkaTemplate) {
        this.hubKafkaTemplate = hubKafkaTemplate;
        this.sensorKafkaTemplate = sensorKafkaTemplate;
    }

    @Override
    public void collectHubEvent(HubEvent request, StreamObserver<CollectorResponse> responseObserver) {

        DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder().setId("sensor-123").setType(DeviceTypeAvro.MOTION_SENSOR).build();

        HubEventAvro avroEvent = HubEventAvro.newBuilder().setHubId(request.getHubId()).setTimestamp(Instant.now()).setPayload(payload).build();

        hubKafkaTemplate.send("telemetry.hubs.v1", avroEvent.getHubId(), avroEvent);

        CollectorResponse response = CollectorResponse.newBuilder().setSuccess(true).setMessage("Hub event received and sent to Kafka").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void collectSensorEvent(telemetry.service.collector.SensorEvent request, StreamObserver<CollectorResponse> responseObserver) {

        ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro motionPayload = ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro.newBuilder().setLinkQuality(255).setMotion(true).setVoltage(3000).build();

        SensorEventAvro avroEvent = SensorEventAvro.newBuilder().setId(request.getSensorId()).setHubId(request.getHubId()).setTimestamp(Instant.now()).setPayload(motionPayload).build();

        sensorKafkaTemplate.send("telemetry.sensors.v1", avroEvent.getId(), avroEvent);

        CollectorResponse response = CollectorResponse.newBuilder().setSuccess(true).setMessage("Sensor event received and sent to Kafka").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}