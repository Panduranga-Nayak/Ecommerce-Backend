package com.scaler.notificationservice.email;

import com.scaler.notificationservice.model.EmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EmailSenderRouter implements EmailSender {
    private final String mode;
    private final SmtpEmailSender smtpEmailSender;
    private final LogEmailSender logEmailSender;

    public EmailSenderRouter(@Value("${notification.email.mode}") String mode,
                             SmtpEmailSender smtpEmailSender,
                             LogEmailSender logEmailSender) {
        this.mode = mode;
        this.smtpEmailSender = smtpEmailSender;
        this.logEmailSender = logEmailSender;
    }

    @Override
    public void send(EmailMessage message) {
        if ("smtp".equalsIgnoreCase(mode)) {
            smtpEmailSender.send(message);
        } else {
            logEmailSender.send(message);
        }
    }
}