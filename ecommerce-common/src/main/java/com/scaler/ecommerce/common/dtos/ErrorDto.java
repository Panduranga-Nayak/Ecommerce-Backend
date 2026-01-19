package com.scaler.ecommerce.common.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ErrorDto {
    private String status;
    private String message;
    private String errorCode;
    private String path;
    private String correlationId;
    private Instant timestamp;
    private List<String> details;
}
