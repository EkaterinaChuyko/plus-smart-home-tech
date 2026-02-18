package ru.yandex.practicum.kafka.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.kafka.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Service
@RequiredArgsConstructor
public class KafkaHubEventService {

    private final KafkaTemplate<String, HubEventAvro> kafkaTemplate;
    private final HubEventMapper mapper;

    public void sendHubEvent(HubEvent event) {
        HubEventAvro avroEvent = mapper.toAvro(event);
        kafkaTemplate.send("telemetry.hubs.v1", avroEvent.getHubId(), avroEvent);
    }
}
