package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.processor.HubRouterProcessor;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterProcessor hubRouterProcessor;
    private final ScenarioAnalyzerService scenarioAnalyzer;

    @Transactional
    public void analyze(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId().toString();
        log.info("=== Analyzing snapshot for hub: {} ===", hubId);
        log.info("Snapshot timestamp: {}, sensors count: {}", snapshot.getTimestamp(), snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Found {} scenarios for hub {}", scenarios.size(), hubId);

        if (scenarios.isEmpty()) {
            log.warn("No scenarios found for hub {}", hubId);
            return;
        }

        for (Scenario scenario : scenarios) {
            int conditionsCount = scenario.getScenarioConditions().size();
            int actionsCount = scenario.getScenarioActions().size();

            log.debug("Scenario '{}': loaded {} conditions and {} actions", scenario.getName(), conditionsCount, actionsCount);
        }

        for (Scenario scenario : scenarios) {
            log.info("Checking scenario: '{}' with {} conditions and {} actions", scenario.getName(), scenario.getScenarioConditions().size(), scenario.getScenarioActions().size());

            boolean activated = scenarioAnalyzer.checkScenario(scenario, snapshot);
            log.info("Scenario '{}' activated: {}", scenario.getName(), activated);

            if (activated) {
                log.info("Scenario '{}' ACTIVATED for hub {}", scenario.getName(), hubId);

                for (ScenarioAction scenarioAction : scenario.getScenarioActions()) {
                    log.info("Executing action: sensor={}, type={}, value={}", scenarioAction.getSensor().getId(), scenarioAction.getAction().getType(), scenarioAction.getAction().getValue());

                    hubRouterProcessor.executeAction(scenarioAction, hubId, scenario.getName());
                }
            }
        }
    }
}