package ru.yandex.practicum.aggregator.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public void start() {
        consumer.subscribe(List.of("telemetry.sensors.v1"));

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    Optional<SensorsSnapshotAvro> updatedSnapshot = updateState(record.value());
                    updatedSnapshot.ifPresent(snapshot -> producer.send(new ProducerRecord<>("telemetry.snapshots.v1", snapshot.getHubId(), snapshot)));
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } finally {
            producer.flush();
            consumer.close();
            producer.close();
        }
    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();

        snapshots.putIfAbsent(hubId, new SensorsSnapshotAvro(hubId, event.getTimestamp(), new HashMap<>()));
        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());

        if (oldState != null) {
            if (!event.getTimestamp().isAfter(oldState.getTimestamp()) &&
                oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = new SensorStateAvro(event.getTimestamp(), event.getPayload());
        snapshot.getSensorsState().put(event.getId(), newState);

        snapshot.setTimestamp(event.getTimestamp());

        return Optional.of(snapshot);
    }
}
