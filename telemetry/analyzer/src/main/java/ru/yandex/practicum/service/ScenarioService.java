package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.Sensor;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;

    public ScenarioService(ScenarioRepository scenarioRepository, SensorRepository sensorRepository) {
        this.scenarioRepository = scenarioRepository;
        this.sensorRepository = sensorRepository;
    }

    public List<Scenario> getScenariosByHub(String hubId) {
        return scenarioRepository.findByHubId(hubId);
    }

    public Scenario saveOrUpdate(Scenario scenario) {
        return scenarioRepository.save(scenario);
    }

    public Sensor saveOrUpdateSensor(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    public Optional<Sensor> findSensor(String sensorId, String hubId) {
        return sensorRepository.findByIdAndHubId(sensorId, hubId);
    }
}
