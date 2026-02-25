package ru.yandex.practicum.kafka.telemetry.collector.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.KafkaHubEventService;

@RestController
@RequestMapping("/events/hubs")
public class HubController {

    private final KafkaHubEventService kafkaHubEventService;

    public HubController(KafkaHubEventService kafkaHubEventService) {
        this.kafkaHubEventService = kafkaHubEventService;
    }

    @PostMapping
    public ResponseEntity<Void> collect(@Valid @RequestBody HubEvent event) {
        kafkaHubEventService.sendHubEvent(event);
        return ResponseEntity.accepted().build();
    }
}