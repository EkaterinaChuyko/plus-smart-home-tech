package ru.yandex.practicum.kafka.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaProducer<String, SensorEventAvro> sensorEventProducer;
    private final KafkaProducer<String, HubEventAvro> hubEventProducer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        String key = event.getHubId() + "_" + event.getId();
        ProducerRecord<String, SensorEventAvro> record =
                new ProducerRecord<>(sensorsTopic, key, event);

        sensorEventProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки события датчика в Kafka", exception);
            } else {
                log.debug("Событие датчика отправлено: partition={}, offset={}",
                        metadata.partition(), metadata.offset());
            }
        });
        sensorEventProducer.flush();
    }

    public void sendHubEvent(HubEventAvro event) {
        String key = event.getHubId();
        ProducerRecord<String, HubEventAvro> record =
                new ProducerRecord<>(hubsTopic, key, event);

        hubEventProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки события хаба в Kafka", exception);
            } else {
                log.debug("Событие хаба отправлено: partition={}, offset={}",
                        metadata.partition(), metadata.offset());
            }
        });
        hubEventProducer.flush();
    }
}