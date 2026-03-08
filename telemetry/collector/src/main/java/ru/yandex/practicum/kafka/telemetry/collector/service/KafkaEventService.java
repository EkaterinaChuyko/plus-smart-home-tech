package ru.yandex.practicum.kafka.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaTemplate<String, SensorEventAvro> sensorKafkaTemplate;
    private final KafkaTemplate<String, HubEventAvro> hubKafkaTemplate;
    private final SensorEventMapper mapper;

    public void sendSensorEvent(SensorEventAvro avroEvent) {
        sensorKafkaTemplate.send("telemetry.sensors.v1", avroEvent.getId(), avroEvent);
    }

    public void sendHubEvent(HubEventAvro event) {
        hubKafkaTemplate.send("telemetry.hubs.v1", event.getHubId(), event);
    }
}