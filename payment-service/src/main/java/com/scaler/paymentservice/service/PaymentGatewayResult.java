package com.scaler.paymentservice.service;

import com.scaler.paymentservice.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentGatewayResult {
    private PaymentStatus status;
    private String providerReference;
    private String failureReason;

    public static PaymentGatewayResult pending(String providerReference) {
        return build(PaymentStatus.PENDING, providerReference, null);
    }

    public static PaymentGatewayResult completed(String providerReference) {
        return build(PaymentStatus.COMPLETED, providerReference, null);
    }

    public static PaymentGatewayResult failed(String failureReason) {
        return build(PaymentStatus.FAILED, null, failureReason);
    }

    private static PaymentGatewayResult build(PaymentStatus status, String providerReference, String failureReason) {
        PaymentGatewayResult result = new PaymentGatewayResult();
        result.setStatus(status);
        result.setProviderReference(providerReference);
        result.setFailureReason(failureReason);
        return result;
    }
}