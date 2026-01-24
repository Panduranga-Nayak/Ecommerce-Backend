package com.scaler.paymentservice.controller;

import com.scaler.paymentservice.service.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class StripeWebhookController {
    private final StripeWebhookService webhookService;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(@RequestHeader("Stripe-Signature") String signature,
                                                    @RequestBody String payload) {
        webhookService.handleEvent(payload, signature);
        return ResponseEntity.ok().build();
    }
}