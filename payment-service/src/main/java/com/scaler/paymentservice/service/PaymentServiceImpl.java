package com.scaler.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.ecommerce.common.events.OrderCreatedEvent;
import com.scaler.ecommerce.common.events.PaymentCompletedEvent;
import com.scaler.ecommerce.common.events.PaymentFailedEvent;
import com.scaler.ecommerce.common.events.PaymentReceiptEvent;
import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.ecommerce.common.utils.HashUtils;
import com.scaler.paymentservice.dto.PaymentRequestDto;
import com.scaler.paymentservice.exception.AccessDeniedException;
import com.scaler.paymentservice.exception.IdempotencyConflictException;
import com.scaler.paymentservice.exception.InvalidPaymentException;
import com.scaler.paymentservice.exception.PaymentNotFoundException;
import com.scaler.paymentservice.model.enums.PaymentMethod;
import com.scaler.paymentservice.model.enums.PaymentStatus;
import com.scaler.paymentservice.model.IdempotencyKey;
import com.scaler.paymentservice.model.Payment;
import com.scaler.paymentservice.kafka.PaymentEventPublisher;
import com.scaler.paymentservice.repo.IdempotencyKeyRepository;
import com.scaler.paymentservice.repo.PaymentRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              IdempotencyKeyRepository idempotencyKeyRepository,
                              PaymentGateway paymentGateway,
                              PaymentEventPublisher paymentEventPublisher,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.paymentGateway = paymentGateway;
        this.paymentEventPublisher = paymentEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Payment createPayment(PaymentRequestDto request, Long userId, String idempotencyKey) {
        validateRequest(request);

        String requestHash = hashRequest(request, userId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey existing = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);
            if (existing != null) {
                if (!existing.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key already used with different payload");
                }
                if (existing.getPaymentId() != null) {
                    return paymentRepository.findById(existing.getPaymentId())
                            .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
                }
            } else {
                IdempotencyKey record = new IdempotencyKey();
                record.setKey(idempotencyKey);
                record.setOrderId(request.getOrderId());
                record.setRequestHash(requestHash);
                record.setCreatedAt(Instant.now());
                idempotencyKeyRepository.save(record);
            }
        }

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(userId);
        AuthenticatedUser currentUser = RequestContext.getCurrentUser();
        if (currentUser != null) {
            payment.setCustomerEmail(currentUser.getEmail());
        }
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(request.getMethod());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());

        Payment saved = paymentRepository.save(payment);
        processPayment(saved);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey record = idempotencyKeyRepository.findByKey(idempotencyKey).orElse(null);
            if (record != null) {
                record.setPaymentId(saved.getId());
                idempotencyKeyRepository.save(record);
            }
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Payment does not belong to user");
        }

        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Payment does not belong to user");
        }

        return payment;
    }

    @Override
    @Transactional
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        Payment existing = paymentRepository.findByOrderId(event.getOrderId()).orElse(null);
        if (existing != null) {
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setUserId(event.getUserId());
        payment.setCustomerEmail(event.getEmail());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.valueOf(event.getPaymentMethod()));
        payment.setAmount(event.getTotalAmount());
        payment.setCurrency(event.getCurrency());

        Payment saved = paymentRepository.save(payment);
        processPayment(saved);
    }

    private void processPayment(Payment payment) {
        PaymentGatewayResult result = paymentGateway.process(payment);
        if (result.getProviderReference() != null && !result.getProviderReference().isBlank()) {
            payment.setProviderReference(result.getProviderReference());
        }

        PaymentStatus status = result.getStatus() != null ? result.getStatus() : PaymentStatus.PENDING;
        if (status == PaymentStatus.COMPLETED) {
            completePayment(payment, result.getProviderReference(), null);
        } else if (status == PaymentStatus.FAILED) {
            failPayment(payment, result.getFailureReason());
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
        }
    }

    @Override
    @Transactional
    public void markPaymentCompleted(Long paymentId, String providerReference, String receiptNumber) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        completePayment(payment, providerReference, receiptNumber);
    }

    @Override
    @Transactional
    public void markPaymentFailed(Long paymentId, String failureReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        failPayment(payment, failureReason);
    }

    private void completePayment(Payment payment, String providerReference, String receiptNumber) {
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        if (providerReference != null && !providerReference.isBlank()
                && (payment.getProviderReference() == null || payment.getProviderReference().isBlank())) {
            payment.setProviderReference(providerReference);
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        String receipt = receiptNumber;
        if (receipt == null || receipt.isBlank()) {
            receipt = "RCPT-" + payment.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        payment.setReceiptNumber(receipt);
        paymentRepository.save(payment);

        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent();
        completedEvent.setPaymentId(payment.getId());
        completedEvent.setOrderId(payment.getOrderId());
        completedEvent.setUserId(payment.getUserId());
        completedEvent.setMethod(payment.getMethod().name());
        completedEvent.setCurrency(payment.getCurrency());
        completedEvent.setAmount(payment.getAmount());
        completedEvent.setReceiptNumber(payment.getReceiptNumber());

        paymentEventPublisher.publish("payment.completed", getCorrelationId(), completedEvent);

        PaymentReceiptEvent receiptEvent = new PaymentReceiptEvent();
        receiptEvent.setPaymentId(payment.getId());
        receiptEvent.setOrderId(payment.getOrderId());
        receiptEvent.setUserId(payment.getUserId());
        receiptEvent.setReceiptNumber(payment.getReceiptNumber());
        receiptEvent.setEmail(payment.getCustomerEmail());
        paymentEventPublisher.publish("payment.receipt", getCorrelationId(), receiptEvent);
    }

    private void failPayment(Payment payment, String failureReason) {
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason != null ? failureReason : "Payment failed");
        paymentRepository.save(payment);

        PaymentFailedEvent failedEvent = new PaymentFailedEvent();
        failedEvent.setPaymentId(payment.getId());
        failedEvent.setOrderId(payment.getOrderId());
        failedEvent.setUserId(payment.getUserId());
        failedEvent.setMethod(payment.getMethod().name());
        failedEvent.setCurrency(payment.getCurrency());
        failedEvent.setAmount(payment.getAmount());
        failedEvent.setFailureReason(payment.getFailureReason());

        paymentEventPublisher.publish("payment.failed", getCorrelationId(), failedEvent);
    }

    private void validateRequest(PaymentRequestDto request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new InvalidPaymentException("Currency required");
        }
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String hashRequest(PaymentRequestDto request, Long userId) {
        try {
            return HashUtils.sha256(objectMapper.writeValueAsString(request) + ":" + userId);
        } catch (JsonProcessingException e) {
            throw new InvalidPaymentException("Unable to process request");
        }
    }
}