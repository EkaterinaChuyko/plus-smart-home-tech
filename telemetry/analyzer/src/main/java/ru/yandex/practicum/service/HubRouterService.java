package ru.yandex.practicum.service;

import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@GrpcService
public class HubRouterService extends HubRouterControllerGrpc.HubRouterControllerImplBase {
}