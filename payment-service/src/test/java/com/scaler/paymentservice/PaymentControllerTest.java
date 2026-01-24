package com.scaler.paymentservice;

import com.scaler.ecommerce.common.security.AuthenticatedUser;
import com.scaler.ecommerce.common.security.RequestContext;
import com.scaler.paymentservice.controller.PaymentController;
import com.scaler.paymentservice.dto.PaymentRequestDto;
import com.scaler.paymentservice.dto.PaymentResponseDto;
import com.scaler.paymentservice.service.PaymentService;
import com.scaler.paymentservice.model.enums.PaymentMethod;
import com.scaler.paymentservice.model.enums.PaymentStatus;
import com.scaler.paymentservice.model.Payment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    @BeforeEach
    void setup() {
        controller = new PaymentController(paymentService);
    }

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    @Test
    void createPaymentReturnsResponse() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserId(1L);
        user.setEmail("buyer@example.com");
        user.setRoles(java.util.Set.of("CUSTOMER"));
        RequestContext.setCurrentUser(user);

        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(101L);
        request.setAmount(new BigDecimal("499.00"));
        request.setCurrency("USD");
        request.setMethod(PaymentMethod.CARD);

        Payment payment = new Payment();
        payment.setId(55L);
        payment.setOrderId(101L);
        payment.setUserId(1L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.CARD);
        payment.setAmount(new BigDecimal("499.00"));
        payment.setCurrency("USD");
        payment.setReceiptNumber("REC-55");
        payment.setProviderReference("link-55");

        when(paymentService.createPayment(request, 1L, "pay-key")).thenReturn(payment);

        PaymentResponseDto response = controller.createPayment(request, "pay-key");

        assertEquals(55L, response.getPaymentId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("USD", response.getCurrency());
        verify(paymentService).createPayment(request, 1L, "pay-key");
    }
}