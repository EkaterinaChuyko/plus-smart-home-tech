package ru.yandex.practicum.aggregator.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class SensorsSnapshotSerializer implements Serializer<SensorsSnapshotAvro> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, SensorsSnapshotAvro data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<SensorsSnapshotAvro> writer = new SpecificDatumWriter<>(SensorsSnapshotAvro.getClassSchema());
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Ошибка сериализации SensorsSnapshotAvro", e);
            throw new RuntimeException("Ошибка сериализации", e);
        }
    }

    @Override
    public void close() {
    }
}