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
import ru.yandex.practicum.aggregator.SnapshotAggregator;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final SnapshotAggregator snapshotAggregator = new SnapshotAggregator();

    public void start() {
        consumer.subscribe(List.of("telemetry.sensors.v1"));

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    Optional<SensorsSnapshotAvro> updatedSnapshot =
                            snapshotAggregator.updateState(record.value());

                    updatedSnapshot.ifPresent(snapshot ->
                            producer.send(new ProducerRecord<>("telemetry.snapshots.v1",
                                    snapshot.getHubId(), snapshot)));
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
}