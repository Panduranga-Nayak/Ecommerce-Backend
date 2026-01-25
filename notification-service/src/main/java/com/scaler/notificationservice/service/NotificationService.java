package com.scaler.notificationservice.service;

import com.scaler.ecommerce.common.events.OrderStatusUpdatedEvent;
import com.scaler.ecommerce.common.events.PasswordResetRequestedEvent;
import com.scaler.ecommerce.common.events.PaymentReceiptEvent;
import com.scaler.ecommerce.common.events.UserRegisteredEvent;
import com.scaler.notificationservice.model.EmailMessage;
import com.scaler.notificationservice.email.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final EmailSender emailSender;
    private final String fromAddress;

    public NotificationService(EmailSender emailSender,
                               @Value("${notification.email.from}") String fromAddress) {
        this.emailSender = emailSender;
        this.fromAddress = fromAddress;
    }

    public void sendUserRegistered(UserRegisteredEvent event) {
        EmailMessage message = new EmailMessage();
        message.setTo(event.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Welcome to our store");
        message.setBody("Hello " + event.getName() + ", your account is ready.");
        emailSender.send(message);
    }

    public void sendPasswordReset(PasswordResetRequestedEvent event) {
        EmailMessage message = new EmailMessage();
        message.setTo(event.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Password reset request");
        message.setBody("Use this link to reset your password: " + event.getResetUrl());
        emailSender.send(message);
    }

    public void sendOrderStatusUpdate(OrderStatusUpdatedEvent event) {
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            return;
        }
        EmailMessage message = new EmailMessage();
        message.setTo(event.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Order update: " + event.getStatus());
        message.setBody("Your order " + event.getOrderId() + " is now " + event.getStatus()
                + ". " + (event.getDescription() != null ? event.getDescription() : ""));
        emailSender.send(message);
    }

    public void sendPaymentReceipt(PaymentReceiptEvent event) {
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            return;
        }
        EmailMessage message = new EmailMessage();
        message.setTo(event.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Payment receipt " + event.getReceiptNumber());
        message.setBody("Payment received for order " + event.getOrderId() + ". Receipt: " + event.getReceiptNumber());
        emailSender.send(message);
    }
}