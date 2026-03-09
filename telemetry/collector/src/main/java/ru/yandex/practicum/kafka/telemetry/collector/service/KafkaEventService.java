package ru.yandex.practicum.kafka.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.sensors:telemetry.sensors.v1}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs:telemetry.hubs.v1}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        log.info("Sending sensor event to topic {}: sensorId={}", sensorsTopic, event.getId());
        kafkaTemplate.send(sensorsTopic, event.getId(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        log.info("Sending hub event to topic {}: hubId={}", hubsTopic, event.getHubId());
        kafkaTemplate.send(hubsTopic, event.getHubId(), event);
    }
}