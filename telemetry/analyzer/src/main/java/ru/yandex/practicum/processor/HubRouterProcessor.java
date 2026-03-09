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
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class HubRouterProcessor {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;
    private final String targetAddress;

    public HubRouterProcessor(@Value("${grpc.client.hub-router.address}") String address) {
        log.info("========== CREATING HUB ROUTER PROCESSOR ==========");
        log.info("Configured address: {}", address);
        this.targetAddress = address;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
        this.hubRouterClient = HubRouterControllerGrpc.newBlockingStub(channel);

        log.info("Channel created for target: {}", address);
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
            log.warn("Port may be incorrect. Expected 59090, got: {}", actualAddress);
        }

        try {
            log.info("Testing connection to hub-router on {}", actualAddress);
            if (hubRouterClient != null) {
                log.info("gRPC client initialized successfully");
            }
        } catch (Exception e) {
            log.error("Connection test failed", e);
        }
    }

    public Empty executeAction(ScenarioAction scenarioAction, String hubId, String scenarioName) {
        log.info("========== EXECUTING ACTION ==========");
        log.info("Hub ID: {}", hubId);
        log.info("Scenario name: {}", scenarioName);

        if (scenarioAction == null) {
            log.error("scenarioAction is null");
            return null;
        }

        Action action = scenarioAction.getAction();
        Sensor sensor = scenarioAction.getSensor();

        log.info("Sensor ID: {}", sensor != null ? sensor.getId() : "null");
        log.info("Action type: {}", action != null ? action.getType() : "null");
        log.info("Action value: {}", action != null ? action.getValue() : "null");
        log.info("gRPC target address: {}", targetAddress);
        log.info("Channel authority: {}", hubRouterClient.getChannel().authority());

        if (sensor == null) {
            log.error("Action has no associated sensor");
            return null;
        }

        if (action == null) {
            log.error("Action is null");
            return null;
        }

        try {
            log.info("Building DeviceActionProto...");
            DeviceActionProto hubActionProto = DeviceActionProto.newBuilder()
                    .setSensorId(sensor.getId())
                    .setType(ActionTypeProto.valueOf(action.getType().name()))
                    .setValue(action.getValue() != null ? action.getValue() : 0)
                    .build();

            log.info("Building DeviceActionRequest...");
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(hubActionProto)
                    .build();

            log.info("Sending gRPC request to {} on port 59090",
                    hubRouterClient.getChannel().authority());
            log.debug("Request details: hubId={}, scenario={}, sensor={}, type={}, value={}",
                    request.getHubId(), request.getScenarioName(),
                    request.getAction().getSensorId(),
                    request.getAction().getType(),
                    request.getAction().getValue());

            HubRouterControllerGrpc.HubRouterControllerBlockingStub stubWithTimeout =
                    hubRouterClient.withDeadlineAfter(5, TimeUnit.SECONDS);

            Empty response = stubWithTimeout.handleDeviceAction(request);

            log.info("Action sent successfully! Response: {}", response);
            return response;

        } catch (StatusRuntimeException e) {
            log.error("gRPC error: {}", e.getStatus());
            log.error("Error description: {}", e.getStatus().getDescription());
            log.error("Error code: {}", e.getStatus().getCode());

            if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
                log.error("Hub Router is NOT available on localhost:59090!");
                log.error("Possible causes:");
                log.error("  1. Hub Router is not running in the test environment");
                log.error("  2. Firewall blocking port 59090");
                log.error("  3. Wrong port configuration (current: 59090)");
            } else if (e.getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                log.error("Hub Router timeout - service too slow or not responding");
            } else if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                log.error("Hub Router service method not implemented");
            }

            log.error("Stack trace:", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error sending action", e);
            return null;
        }
    }
}