package ru.yandex.practicum.kafka.telemetry.collector.serialization;

import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public class CombinedEventSerializer implements Serializer<Object> {

    private final SensorEventSerializer sensorSerializer = new SensorEventSerializer();
    private final HubEventSerializer hubSerializer = new HubEventSerializer();

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof SensorEventAvro) {
            return sensorSerializer.serialize(topic, (SensorEventAvro) data);
        } else if (data instanceof HubEventAvro) {
            return hubSerializer.serialize(topic, (HubEventAvro) data);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + data.getClass().getName());
        }
    }

    @Override
    public void close() {
        sensorSerializer.close();
        hubSerializer.close();
    }
}
