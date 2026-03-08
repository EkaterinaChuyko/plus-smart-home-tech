package ru.yandex.practicum.kafka.telemetry.collector.controller;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import telemetry.service.collector.CollectorGrpc;
import telemetry.service.collector.CollectorResponse;
import telemetry.service.collector.HubEvent;

import java.time.Instant;

@GrpcService
public class CollectorController extends CollectorGrpc.CollectorImplBase {

    private final KafkaTemplate<String, HubEventAvro> hubKafkaTemplate;

    public CollectorController(KafkaTemplate<String, HubEventAvro> hubKafkaTemplate) {
        this.hubKafkaTemplate = hubKafkaTemplate;
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
}