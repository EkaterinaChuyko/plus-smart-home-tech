package ru.yandex.practicum.kafka.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaTemplate<String, SensorEventAvro> kafkaTemplate;
    private final SensorEventMapper mapper;

    public void sendSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = mapper.toAvro(event);
        kafkaTemplate.send("telemetry.sensors.v1", avroEvent.getId(), avroEvent);
    }
}
