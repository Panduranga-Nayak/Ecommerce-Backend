package com.scaler.ecommerce.common.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String name;
    private String registrationMethod;
}
