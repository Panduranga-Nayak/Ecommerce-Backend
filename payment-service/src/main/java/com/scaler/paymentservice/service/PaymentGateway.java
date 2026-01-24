package com.scaler.paymentservice.service;

import com.scaler.paymentservice.model.Payment;

public interface PaymentGateway {
    PaymentGatewayResult process(Payment payment);
}