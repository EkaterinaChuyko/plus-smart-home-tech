package ru.yandex.practicum.aggregator;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotAggregator {

    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(
                hubId,
                id -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(id)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        Map<String, SensorStateAvro> stateMap = snapshot.getSensorsState();
        SensorStateAvro oldState = stateMap.get(event.getId());

        if (oldState != null) {
            boolean isOlder = !event.getTimestamp().isAfter(oldState.getTimestamp());
            boolean isSameData = oldState.getData().equals(event.getPayload());

            if (isOlder || isSameData) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        stateMap.put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());

        return Optional.of(snapshot);
    }
}