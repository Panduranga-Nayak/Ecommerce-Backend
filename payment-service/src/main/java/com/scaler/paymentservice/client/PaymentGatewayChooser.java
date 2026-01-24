package com.scaler.paymentservice.client;

import com.scaler.paymentservice.service.PaymentGateway;
import com.scaler.paymentservice.service.PaymentGatewayResult;
import com.scaler.paymentservice.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class PaymentGatewayChooser implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayChooser.class);

    private final MockPaymentGateway mockPaymentGateway;
    private final String gateway;
    private final StripePaymentGateway stripePaymentGateway;
    private final String stripeSecretKey;

    public PaymentGatewayChooser(StripePaymentGateway stripePaymentGateway,
                                 MockPaymentGateway mockPaymentGateway,
                                 @Value("${payment.gateway:stripe}") String gateway,
                                 @Value("${stripe.secret.key:}") String stripeSecretKey) {
        this.stripePaymentGateway = stripePaymentGateway;
        this.mockPaymentGateway = mockPaymentGateway;
        this.gateway = gateway;
        this.stripeSecretKey = stripeSecretKey;
    }

    @Override
    public PaymentGatewayResult process(Payment payment) {
        if ("stripe".equalsIgnoreCase(gateway) && hasStripeCredentials()) {
            return stripePaymentGateway.process(payment);
        }

        if ("stripe".equalsIgnoreCase(gateway)) {
            log.warn("Stripe credentials missing, falling back to mock gateway");
        }

        return mockPaymentGateway.process(payment);
    }

    private boolean hasStripeCredentials() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }
}