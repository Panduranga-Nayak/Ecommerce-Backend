package com.scaler.notificationservice.controller;

import com.scaler.notificationservice.dto.EmailRequestDto;
import com.scaler.notificationservice.model.EmailMessage;
import com.scaler.notificationservice.email.EmailSender;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final EmailSender emailSender;
    private final String fromAddress;

    public NotificationController(EmailSender emailSender,
                                  @Value("${notification.email.from}") String fromAddress) {
        this.emailSender = emailSender;
        this.fromAddress = fromAddress;
    }

    @PostMapping("/email")
    public void sendEmail(@Valid @RequestBody EmailRequestDto request) {
        EmailMessage message = new EmailMessage();
        message.setTo(request.getTo());
        message.setFrom(fromAddress);
        message.setSubject(request.getSubject());
        message.setBody(request.getBody());
        emailSender.send(message);
    }
}