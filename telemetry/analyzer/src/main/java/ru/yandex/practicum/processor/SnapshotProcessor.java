package ru.yandex.practicum.processor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConfig;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.SnapshotService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class SnapshotProcessor implements Runnable {

    private final KafkaConfig kafkaConfig;
    private final SnapshotService snapshotService;
    private final String groupId;
    private KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;

    public SnapshotProcessor(KafkaConfig kafkaConfig, SnapshotService snapshotService, @Value("${spring.kafka.consumer.snapshot.group-id}") String groupId) {
        this.kafkaConfig = kafkaConfig;
        this.snapshotService = snapshotService;
        this.groupId = groupId;
    }

    @PostConstruct
    public void start() {
        log.info("=== SnapshotProcessor INIT ===");

        snapshotConsumer = kafkaConfig.createSnapshotConsumer(groupId);
        log.info("Topic: {}", kafkaConfig.getSnapshotsTopic());

        new Thread(this, "SnapshotProcessorThread").start();
    }

    @Override
    public void run() {
        log.info("=== SnapshotProcessor START ===");

        // consumer локально для этого потока
        try (KafkaConsumer<String, SensorsSnapshotAvro> consumer = kafkaConfig.createSnapshotConsumer(groupId)) {

            consumer.subscribe(List.of(kafkaConfig.getSnapshotsTopic()));
            while (!Thread.currentThread().isInterrupted()) {
                var records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    snapshotService.analyze(record.value());
                }
                consumer.commitAsync();
            }

        } catch (WakeupException e) {
            log.info("SnapshotProcessor wakeup");
        } catch (Exception e) {
            log.error("Error handling SnapshotEvents from Kafka", e);
        }

        log.info("SnapshotProcessor closed");
    }
}