package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestedEvent {
    private Long userId;
    private String email;
    private String resetToken;
    private String resetUrl;
}
