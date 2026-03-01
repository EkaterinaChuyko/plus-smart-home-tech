package ru.yandex.practicum.aggregator.kafka.serialization;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;

import java.util.Map;

public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try {
            DatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            org.apache.avro.io.Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {}
}