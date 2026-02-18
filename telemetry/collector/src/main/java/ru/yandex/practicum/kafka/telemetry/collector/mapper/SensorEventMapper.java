package ru.yandex.practicum.kafka.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder().setId(event.getId()).setHubId(event.getHubId()).setTimestamp(Instant.ofEpochMilli(event.getTimestamp().toEpochMilli()));

        if (event instanceof ClimateSensorEvent) {
            ClimateSensorEvent e = (ClimateSensorEvent) event;
            ClimateSensorAvro payload = ClimateSensorAvro.newBuilder().setTemperatureC(e.getTemperatureC()).setHumidity(e.getHumidity()).setCo2Level(e.getCo2Level()).build();
            builder.setPayload(payload);

        } else if (event instanceof LightSensorEvent) {
            LightSensorEvent e = (LightSensorEvent) event;
            LightSensorAvro payload = LightSensorAvro.newBuilder().setLinkQuality(e.getLinkQuality()).setLuminosity(e.getLuminosity()).build();
            builder.setPayload(payload);

        } else if (event instanceof MotionSensorEvent) {
            MotionSensorEvent e = (MotionSensorEvent) event;
            MotionSensorAvro payload = MotionSensorAvro.newBuilder().setLinkQuality(e.getLinkQuality()).setMotion(e.isMotion()).setVoltage(e.getVoltage()).build();
            builder.setPayload(payload);

        } else if (event instanceof SwitchSensorEvent) {
            SwitchSensorEvent e = (SwitchSensorEvent) event;
            SwitchSensorAvro payload = SwitchSensorAvro.newBuilder().setState(e.isState()).build();
            builder.setPayload(payload);

        } else if (event instanceof TemperatureSensorEvent) {
            TemperatureSensorEvent e = (TemperatureSensorEvent) event;
            TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder().setTemperatureC(e.getTemperatureC()).setTemperatureF(e.getTemperatureF()).build();
            builder.setPayload(payload);
        }

        return builder.build();
    }
}

