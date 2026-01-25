package com.scaler.notificationservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailMessage {
    private String to;
    private String from;
    private String subject;
    private String body;
}