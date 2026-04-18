package ru.yandex.practicum.exception;

public class DeliveryCalculationException extends RuntimeException {
    public DeliveryCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}