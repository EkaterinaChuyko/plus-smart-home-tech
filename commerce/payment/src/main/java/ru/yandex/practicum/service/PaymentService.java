package ru.yandex.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.ProductClient;
import ru.yandex.practicum.dto.order.OrderRequest;
import ru.yandex.practicum.dto.order.TotalCostRequest;
import ru.yandex.practicum.dto.payment.PaymentRequest;
import ru.yandex.practicum.enums.PaymentStatus;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final OrderClient orderClient;
    private final ProductClient productClient;

    public Double calculateProductsCost(OrderRequest request) {

        return request.getItems().stream().mapToDouble(item -> {
            var product = productClient.getProduct(item.getProductId());

            if (product == null) {
                throw new IllegalStateException("Product not found: " + item.getProductId());
            }

            return product.getPrice() * item.getQuantity();
        }).sum();
    }

    public Double calculateTotalCost(TotalCostRequest request) {

        double products = request.getProductsCost();
        double tax = products * 0.1;
        return products + tax + request.getDeliveryCost();
    }

    public UUID createPayment(PaymentRequest request) {

        double products = request.getProductsPrice();
        double tax = products * 0.1;
        double total = products + tax + request.getDeliveryPrice();

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setProductsPrice(products);
        payment.setDeliveryPrice(request.getDeliveryPrice());
        payment.setTotalPrice(total);
        payment.setStatus(PaymentStatus.PENDING);

        return repository.save(payment).getId();
    }

    public void success(UUID paymentId) {

        Payment payment = repository.findById(paymentId).orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.SUCCESS);
        repository.save(payment);

        orderClient.paymentSuccess(payment.getOrderId());
    }

    public void failed(UUID paymentId) {

        Payment payment = repository.findById(paymentId).orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        repository.save(payment);

        orderClient.paymentFailed(payment.getOrderId());
    }
}