package ru.yandex.practicum.service;

import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.entity.ScenarioCondition;
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
        log.info("========== ANALYZING SNAPSHOT FOR HUB {} ==========", hubId);
        log.info("Snapshot timestamp: {}, sensors count: {}",
                snapshot.getTimestamp(), snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Found {} scenarios for hub {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            log.info("----------------------------------------");
            log.info("Checking scenario: '{}'", scenario.getName());
            log.info("Conditions count: {}", scenario.getScenarioConditions().size());
            log.info("Actions count: {}", scenario.getScenarioActions().size());

            boolean activated = scenarioAnalyzer.checkScenario(scenario, snapshot);
            log.info("Scenario '{}' activated: {}", scenario.getName(), activated);

            if (activated) {
                log.info("🎯 SCENARIO ACTIVATED! Executing {} actions...",
                        scenario.getScenarioActions().size());

                for (ScenarioAction scenarioAction : scenario.getScenarioActions()) {
                    log.info(">>> Calling executeAction for sensor: {}, type: {}, value: {}",
                            scenarioAction.getSensor().getId(),
                            scenarioAction.getAction().getType(),
                            scenarioAction.getAction().getValue());

                    try {
                        Empty result = hubRouterProcessor.executeAction(
                                scenarioAction,
                                hubId,
                                scenario.getName()
                        );

                        if (result != null) {
                            log.info("✅ executeAction SUCCESS for sensor: {}",
                                    scenarioAction.getSensor().getId());
                        } else {
                            log.error("❌ executeAction FAILED for sensor: {}",
                                    scenarioAction.getSensor().getId());
                        }
                    } catch (Exception e) {
                        log.error("❌ Exception in executeAction for sensor: {}",
                                scenarioAction.getSensor().getId(), e);
                    }
                }
            } else {
                log.info("❌ Scenario NOT activated - conditions not met");

                for (ScenarioCondition condition : scenario.getScenarioConditions()) {
                    log.info("  Condition: sensor={}, type={}, op={}, expected={}",
                            condition.getSensor().getId(),
                            condition.getCondition().getType(),
                            condition.getCondition().getOperation(),
                            condition.getCondition().getValue());
                }
            }
        }
    }
}