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
    private final String targetAddress;

    public HubRouterProcessor(@Value("${grpc.client.hub-router.address}") String address) {
        log.info("========== CREATING HUB ROUTER PROCESSOR ==========");
        log.info("Configured address: {}", address);
        this.targetAddress = address;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        this.hubRouterClient = HubRouterControllerGrpc.newBlockingStub(channel);

        log.info("Channel created for target: {}", address);
        log.info("HubRouterProcessor created successfully");
    }

    @PostConstruct
    public void init() {
        log.info("========== HUB ROUTER PROCESSOR INIT ==========");
        log.info("gRPC client configured with address: {}", targetAddress);

        String actualAddress = hubRouterClient.getChannel().authority();
        log.info("Actual channel authority: {}", actualAddress);

        if (actualAddress != null && actualAddress.contains("59090")) {
            log.info("Port 59090 is correctly configured");
        } else {
            log.warn("⚠Port may be incorrect. Expected 59090, got: {}", actualAddress);
        }
    }

    public Empty executeAction(ScenarioAction scenarioAction, String hubId, String scenarioName) {
        Action action = scenarioAction.getAction();
        Sensor sensor = scenarioAction.getSensor();

        log.info("========== EXECUTING ACTION ==========");
        log.info("Hub ID: {}", hubId);
        log.info("Scenario name: {}", scenarioName);
        log.info("Sensor ID: {}", sensor != null ? sensor.getId() : "null");
        log.info("Action type: {}", action.getType());
        log.info("Action value: {}", action.getValue());
        log.info("gRPC target address: {}", targetAddress);
        log.info("Channel authority: {}", hubRouterClient.getChannel().authority());

        if (sensor == null) {
            log.error("Action has no associated sensor: {}", action.getId());
            return null;
        }

        log.info("Sending command: sensor={}, type={}, value={}", sensor.getId(), action.getType(), action.getValue());

        try {
            DeviceActionProto hubActionProto = DeviceActionProto.newBuilder().setSensorId(sensor.getId()).setType(HubRouter.ActionTypeProto.valueOf(action.getType().name())).setValue(action.getValue() != null ? action.getValue() : 0).build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder().setHubId(hubId).setScenarioName(scenarioName).setAction(hubActionProto).build();

            log.info("Sending gRPC request to hub-router on {}", hubRouterClient.getChannel().authority());
            log.debug("Request details: {}", request);

            Empty response = hubRouterClient.handleDeviceAction(request);

            log.info("Action sent successfully! Response: {}", response);
            return response;

        } catch (StatusRuntimeException e) {
            log.error("gRPC error: {}", e.getStatus());
            log.error("Error description: {}", e.getStatus().getDescription());
            log.error("Error cause", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error sending action", e);
            return null;
        }
    }
}