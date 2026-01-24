package com.scaler.paymentservice.service;

import com.scaler.paymentservice.exception.InvalidWebhookException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeWebhookService {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private final PaymentService paymentService;
    private final String webhookSecret;

    public StripeWebhookService(PaymentService paymentService,
                                @Value("${stripe.webhook.secret:}") String webhookSecret) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    public void handleEvent(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new InvalidWebhookException("Stripe webhook secret not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidWebhookException("Missing Stripe-Signature header");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new InvalidWebhookException("Invalid Stripe webhook signature");
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);
        if (stripeObject == null) {
            log.warn("Stripe webhook missing data object for type {}", event.getType());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(stripeObject);
            case "checkout.session.expired" -> handleCheckoutSessionExpired(stripeObject);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(stripeObject);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(stripeObject);
            default -> log.debug("Ignoring Stripe event {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(StripeObject stripeObject) {
        if (!(stripeObject instanceof Session session)) {
            return;
        }

        String paymentStatus = session.getPaymentStatus();
        if (paymentStatus != null && !"paid".equalsIgnoreCase(paymentStatus)) {
            log.debug("Stripe checkout session not paid (status={})", paymentStatus);
            return;
        }

        Long paymentId = extractPaymentId(session.getMetadata(), session.getClientReferenceId());
        if (paymentId == null) {
            log.warn("Stripe checkout session missing paymentId metadata");
            return;
        }

        paymentService.markPaymentCompleted(paymentId, session.getUrl(), null);
    }

    private void handleCheckoutSessionExpired(StripeObject stripeObject) {
        if (!(stripeObject instanceof Session session)) {
            return;
        }

        Long paymentId = extractPaymentId(session.getMetadata(), session.getClientReferenceId());
        if (paymentId == null) {
            log.warn("Stripe checkout session missing paymentId metadata (expired)");
            return;
        }

        paymentService.markPaymentFailed(paymentId, "Checkout session expired");
    }

    private void handlePaymentIntentSucceeded(StripeObject stripeObject) {
        if (!(stripeObject instanceof PaymentIntent intent)) {
            return;
        }

        Long paymentId = extractPaymentId(intent.getMetadata(), null);
        if (paymentId == null) {
            log.warn("Stripe payment intent missing paymentId metadata (succeeded)");
            return;
        }

        paymentService.markPaymentCompleted(paymentId, intent.getId(), null);
    }

    private void handlePaymentIntentFailed(StripeObject stripeObject) {
        if (!(stripeObject instanceof PaymentIntent intent)) {
            return;
        }

        Long paymentId = extractPaymentId(intent.getMetadata(), null);
        if (paymentId == null) {
            log.warn("Stripe payment intent missing paymentId metadata (failed)");
            return;
        }

        String reason = "Payment failed";
        if (intent.getLastPaymentError() != null && intent.getLastPaymentError().getMessage() != null) {
            reason = intent.getLastPaymentError().getMessage();
        }

        paymentService.markPaymentFailed(paymentId, reason);
    }

    private Long extractPaymentId(Map<String, String> metadata, String fallback) {
        String candidate = metadata != null ? metadata.get("paymentId") : null;
        if (candidate == null || candidate.isBlank()) {
            candidate = fallback;
        }
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(candidate);
        } catch (NumberFormatException ex) {
            log.warn("Invalid paymentId metadata {}", candidate);
            return null;
        }
    }
}