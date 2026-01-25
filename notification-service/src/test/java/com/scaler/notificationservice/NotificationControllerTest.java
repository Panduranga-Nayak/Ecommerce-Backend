package com.scaler.notificationservice;

import com.scaler.notificationservice.controller.NotificationController;
import com.scaler.notificationservice.dto.EmailRequestDto;
import com.scaler.notificationservice.model.EmailMessage;
import com.scaler.notificationservice.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock
    private EmailSender emailSender;

    @Test
    void sendEmailBuildsMessage() {
        NotificationController controller = new NotificationController(emailSender, "no-reply@example.com");

        EmailRequestDto request = new EmailRequestDto();
        request.setTo("test@example.com");
        request.setSubject("Hello");
        request.setBody("Test message");

        controller.sendEmail(request);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(captor.capture());

        EmailMessage message = captor.getValue();
        assertEquals("test@example.com", message.getTo());
        assertEquals("no-reply@example.com", message.getFrom());
        assertEquals("Hello", message.getSubject());
        assertEquals("Test message", message.getBody());
    }
}