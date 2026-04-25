package ru.yandex.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.ProductClient;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;
import ru.yandex.practicum.enums.PaymentStatus;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final OrderClient orderClient;
    private final ProductClient productClient;

    public BigDecimal calculateProductsCost(OrderRequest request) {

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        return request.getItems().stream()
                .filter(Objects::nonNull)
                .map(item -> {

                    var product = productClient.getProduct(item.getProductId());

                    if (product == null || product.getPrice() == null) {
                        throw new IllegalStateException("Invalid product data: " + item.getProductId());
                    }

                    return product.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalCost(TotalCostRequest request) {

        BigDecimal products = request.getProductsCost();

        BigDecimal tax = products.multiply(BigDecimal.valueOf(0.1));

        return products
                .add(tax)
                .add(request.getDeliveryCost());
    }

    public UUID createPayment(PaymentRequest request) {

        BigDecimal products = request.getProductsPrice();

        BigDecimal tax = products.multiply(BigDecimal.valueOf(0.1));

        BigDecimal total = products
                .add(tax)
                .add(request.getDeliveryPrice());

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setProductsPrice(products);
        payment.setDeliveryPrice(request.getDeliveryPrice());
        payment.setTotalPrice(total);
        payment.setStatus(PaymentStatus.PENDING);

        return repository.save(payment).getId();
    }

    @Transactional
    public void success(UUID paymentId) {

        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.SUCCESS);
        repository.save(payment);

        try {
            orderClient.paymentSuccess(payment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to notify order service about payment success", e);
            throw new RuntimeException("Order service call failed", e);
        }
    }

    @Transactional
    public void failed(UUID paymentId) {

        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        repository.save(payment);

        try {
            orderClient.paymentFailed(payment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to notify order service about payment failure", e);
            throw new RuntimeException("Order service call failed", e);
        }
    }
}