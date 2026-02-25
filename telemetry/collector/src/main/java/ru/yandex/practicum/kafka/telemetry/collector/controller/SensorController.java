package ru.yandex.practicum.kafka.telemetry.collector.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.KafkaEventService;

@RestController
@RequestMapping("/events/sensors")
public class SensorController {

    private final KafkaEventService kafkaEventService;

    public SensorController(KafkaEventService kafkaEventService) {
        this.kafkaEventService = kafkaEventService;
    }

    @PostMapping
    public ResponseEntity<Void> collect(@Valid @RequestBody SensorEvent event) {
        kafkaEventService.sendSensorEvent(event);
        return ResponseEntity.ok().build();
    }
}
