package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final AggregationService aggregationService;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.snapshots}")
    private String snapshotsTopic;

    public void start() {
        consumer.subscribe(List.of(sensorsTopic));
        log.info("Subscribed to topic: {}", sensorsTopic);

        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();

                    Optional<SensorsSnapshotAvro> snapshotOpt = aggregationService.updateState(event);

                    if (snapshotOpt.isPresent()) {
                        SensorsSnapshotAvro snapshot = snapshotOpt.get();
                        ProducerRecord<String, SensorsSnapshotAvro> producerRecord = new ProducerRecord<>(snapshotsTopic, snapshot.getHubId().toString(), snapshot);

                        producer.send(producerRecord, (metadata, exception) -> {
                            if (exception != null) {
                                log.error("Error sending snapshot to Kafka", exception);
                            }
                        });
                    }

                    currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                    currentOffsets.clear();
                }
            }
        } catch (WakeupException e) {
            log.info("Received shutdown signal");
        } catch (Exception e) {
            log.error("Error during aggregation", e);
        } finally {
            try {
                producer.flush();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }
}