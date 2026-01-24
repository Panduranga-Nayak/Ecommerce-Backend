package com.scaler.paymentservice.client;

import com.scaler.paymentservice.service.PaymentGateway;
import com.scaler.paymentservice.service.PaymentGatewayResult;
import com.scaler.paymentservice.model.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public PaymentGatewayResult process(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            return PaymentGatewayResult.failed("Invalid amount");
        }

        return PaymentGatewayResult.completed("MOCK-" + UUID.randomUUID());
    }
}