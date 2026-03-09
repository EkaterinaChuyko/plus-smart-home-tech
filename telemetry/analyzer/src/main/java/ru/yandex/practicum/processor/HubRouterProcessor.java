package ru.yandex.practicum.processor;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.entity.Sensor;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouter;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouter.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouter.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Slf4j
@Service
public class HubRouterProcessor {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public HubRouterProcessor(@Value("${grpc.client.hub-router.address}") String address) {
        log.info("Creating HubRouterProcessor with address: {}", address);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        this.hubRouterClient = HubRouterControllerGrpc.newBlockingStub(channel);
        log.info("HubRouterProcessor created successfully");
    }

    @PostConstruct
    public void init() {
        log.info("=== HubRouterProcessor INIT ===");
    }

    public Empty executeAction(ScenarioAction scenarioAction, String hubId, String scenarioName) {
        Action action = scenarioAction.getAction();
        Sensor sensor = scenarioAction.getSensor();

        if (sensor == null) {
            log.error("Action has no associated sensor: {}", action.getId());
            return null;
        }

        log.info("Sending command: sensor={}, type={}, value={}", sensor.getId(), action.getType(), action.getValue());

        DeviceActionProto hubActionProto = DeviceActionProto.newBuilder().setSensorId(sensor.getId()).setType(HubRouter.ActionTypeProto.valueOf(action.getType().name())).setValue(action.getValue() != null ? action.getValue() : 0).build();

        DeviceActionRequest request = DeviceActionRequest.newBuilder().setHubId(hubId).setScenarioName(scenarioName).setAction(hubActionProto).build();

        try {
            log.debug("Sending gRPC request to hub-router");
            return hubRouterClient.handleDeviceAction(request);
        } catch (StatusRuntimeException e) {
            log.error("Error sending DeviceActionRequest: {}", e.getMessage(), e);
            return null;
        }
    }
}