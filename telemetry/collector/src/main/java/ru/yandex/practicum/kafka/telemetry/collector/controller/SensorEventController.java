package ru.yandex.practicum.kafka.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.KafkaEventService;

@Slf4j
@RestController
@RequestMapping("/events/sensors")
@RequiredArgsConstructor
public class SensorEventController {

    private final SensorEventMapper sensorEventMapper;
    private final KafkaEventService kafkaEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("Получено событие датчика: {}", event);
        var avroEvent = sensorEventMapper.toAvro(event);
        kafkaEventService.sendSensorEvent(avroEvent);
        log.info("Событие датчика отправлено в Kafka");
    }
}