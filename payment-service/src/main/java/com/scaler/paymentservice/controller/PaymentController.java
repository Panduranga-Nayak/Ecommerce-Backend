package com.scaler.paymentservice.controller;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.paymentservice.dto.PaymentReceiptDto;
import com.scaler.paymentservice.dto.PaymentRequestDto;
import com.scaler.paymentservice.dto.PaymentResponseDto;
import com.scaler.paymentservice.mapper.PaymentMapper;
import com.scaler.paymentservice.service.PaymentService;
import com.scaler.paymentservice.model.Payment;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponseDto createPayment(@Valid @RequestBody PaymentRequestDto request,
                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Payment payment = paymentService.createPayment(request, user.getUserId(), idempotencyKey);
        return PaymentMapper.toResponse(payment);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable Long paymentId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Payment payment = paymentService.getPayment(paymentId, user.getUserId());
        return PaymentMapper.toResponse(payment);
    }

    @GetMapping("/by-order/{orderId}")
    public PaymentResponseDto getPaymentByOrder(@PathVariable Long orderId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Payment payment = paymentService.getPaymentByOrderId(orderId, user.getUserId());
        return PaymentMapper.toResponse(payment);
    }

    @GetMapping("/{paymentId}/receipt")
    public PaymentReceiptDto getReceipt(@PathVariable Long paymentId) {
        AuthenticatedUser user = RequestContext.getCurrentUser();
        Payment payment = paymentService.getPayment(paymentId, user.getUserId());
        return PaymentMapper.toReceipt(payment);
    }
}