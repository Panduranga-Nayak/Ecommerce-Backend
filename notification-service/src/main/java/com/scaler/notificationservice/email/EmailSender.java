package com.scaler.notificationservice.email;

import com.scaler.notificationservice.model.EmailMessage;

public interface EmailSender {
    void send(EmailMessage message);
}