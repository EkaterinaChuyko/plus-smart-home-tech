package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AggregationService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public synchronized Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId().toString();
        String sensorId = event.getId().toString();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, k -> SensorsSnapshotAvro.newBuilder().setHubId(hubId).setTimestamp(Instant.now()).setSensorsState(new HashMap<String, SensorStateAvro>()).build());

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorsState.get(sensorId);

        Instant eventTimestamp = event.getTimestamp();

        if (oldState != null) {
            Instant oldTimestamp = oldState.getTimestamp();

            if (oldTimestamp.isAfter(eventTimestamp)) {
                log.debug("Ignoring outdated event for sensor {}: old timestamp {}, new {}", sensorId, oldTimestamp, eventTimestamp);
                return Optional.empty();
            }

            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Sensor {} data unchanged", sensorId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder().setTimestamp(eventTimestamp).setData(event.getPayload()).build();

        sensorsState.put(sensorId, newState);

        Map<String, SensorStateAvro> updatedSensorsState = new HashMap<>(sensorsState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder().setHubId(snapshot.getHubId()).setTimestamp(eventTimestamp).setSensorsState(updatedSensorsState).build();

        snapshots.put(hubId, updatedSnapshot);

        log.debug("Updated sensor {} state for hub {}", sensorId, hubId);
        return Optional.of(updatedSnapshot);
    }
}