package com.scaler.notificationservice.email;

import com.scaler.notificationservice.model.EmailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender {
    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(message.getTo());
        mail.setFrom(message.getFrom());
        mail.setSubject(message.getSubject());
        mail.setText(message.getBody());
        mailSender.send(mail);
    }
}