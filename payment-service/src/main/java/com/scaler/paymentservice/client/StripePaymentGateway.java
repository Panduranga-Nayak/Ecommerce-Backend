package com.scaler.paymentservice.client;

import com.scaler.paymentservice.service.PaymentGateway;
import com.scaler.paymentservice.service.PaymentGatewayResult;
import com.scaler.paymentservice.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class StripePaymentGateway implements PaymentGateway {
    private final String stripeSecretKey;
    private final String afterCompletionUrl;

    public StripePaymentGateway(@Value("${stripe.secret.key:}") String stripeSecretKey,
                                @Value("${stripe.after-completion.url:https://example.com}") String afterCompletionUrl) {
        this.stripeSecretKey = stripeSecretKey;
        this.afterCompletionUrl = afterCompletionUrl;
    }

    @Override
    public PaymentGatewayResult process(Payment payment) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            return PaymentGatewayResult.failed("Stripe secret key not configured");
        }

        try {
            Stripe.apiKey = stripeSecretKey;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(afterCompletionUrl)
                    .setCancelUrl(afterCompletionUrl)
                    .setClientReferenceId(String.valueOf(payment.getId()))
                    .putMetadata("paymentId", String.valueOf(payment.getId()))
                    .putMetadata("orderId", String.valueOf(payment.getOrderId()))
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(normalizeCurrency(payment.getCurrency()))
                                    .setUnitAmount(toMinorUnits(payment.getAmount()))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Order #" + payment.getOrderId())
                                            .build())
                                    .build())
                            .build())
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata("paymentId", String.valueOf(payment.getId()))
                            .putMetadata("orderId", String.valueOf(payment.getOrderId()))
                            .build())
                    .build();

            Session session = Session.create(params);
            return PaymentGatewayResult.pending(session.getUrl());
        } catch (StripeException e) {
            return PaymentGatewayResult.failed(e.getMessage());
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "usd";
        }
        return currency.toLowerCase(Locale.ROOT);
    }
}
