package com.scaler.userservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserProfileResponseDto {
    private Long id;
    private String name;
    private String email;
    private Set<String> roles;
}