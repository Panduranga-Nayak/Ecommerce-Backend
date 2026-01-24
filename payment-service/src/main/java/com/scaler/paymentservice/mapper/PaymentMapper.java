package com.scaler.paymentservice.mapper;

import com.scaler.paymentservice.dto.PaymentReceiptDto;
import com.scaler.paymentservice.dto.PaymentResponseDto;
import com.scaler.paymentservice.model.Payment;

public class PaymentMapper {
    public static PaymentResponseDto toResponse(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setPaymentId(payment.getId());
        dto.setOrderId(payment.getOrderId());
        dto.setStatus(payment.getStatus());
        dto.setMethod(payment.getMethod());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setReceiptNumber(payment.getReceiptNumber());
        dto.setPaymentLink(payment.getProviderReference());
        return dto;
    }

    public static PaymentReceiptDto toReceipt(Payment payment) {
        PaymentReceiptDto dto = new PaymentReceiptDto();
        dto.setPaymentId(payment.getId());
        dto.setOrderId(payment.getOrderId());
        dto.setReceiptNumber(payment.getReceiptNumber());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setStatus(payment.getStatus());
        return dto;
    }
}