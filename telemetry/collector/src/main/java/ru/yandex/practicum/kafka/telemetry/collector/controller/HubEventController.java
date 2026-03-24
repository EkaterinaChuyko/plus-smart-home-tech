package ru.yandex.practicum.kafka.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.kafka.telemetry.collector.service.KafkaEventService;

@Slf4j
@RestController
@RequestMapping("/events/hubs")
@RequiredArgsConstructor
public class HubEventController {

    private final HubEventMapper hubEventMapper;
    private final KafkaEventService kafkaEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Получено событие хаба: {}", event);
        var avroEvent = hubEventMapper.toAvro(event);
        kafkaEventService.sendHubEvent(avroEvent);
        log.info("Событие хаба отправлено в Kafka");
    }
}