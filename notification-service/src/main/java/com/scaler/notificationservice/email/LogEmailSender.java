package com.scaler.notificationservice.email;

import com.scaler.notificationservice.model.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogEmailSender {
    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    public void send(EmailMessage message) {
        log.info("Email to={} subject={} body={}", message.getTo(), message.getSubject(), message.getBody());
    }
}