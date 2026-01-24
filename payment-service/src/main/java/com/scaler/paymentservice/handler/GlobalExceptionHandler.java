package com.scaler.paymentservice.handler;

import com.scaler.ecommerce.common.dtos.ErrorDto;
import com.scaler.paymentservice.exception.AccessDeniedException;
import com.scaler.paymentservice.exception.IdempotencyConflictException;
import com.scaler.paymentservice.exception.InvalidPaymentException;
import com.scaler.paymentservice.exception.InvalidWebhookException;
import com.scaler.paymentservice.exception.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
@RestController
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorDto> handlePaymentNotFound(PaymentNotFoundException exception,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorDto> handleInvalidPayment(InvalidPaymentException exception,
                                                         HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException exception,
                                                       HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), request, null);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorDto> handleIdempotencyConflict(IdempotencyConflictException exception,
                                                              HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidWebhookException.class)
    public ResponseEntity<ErrorDto> handleInvalidWebhook(InvalidWebhookException exception,
                                                         HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK", exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request,
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException exception,
                                                              HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleUnreadable(HttpMessageNotReadableException exception,
                                                     HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD", "Malformed request body", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnknown(Exception exception, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorDto> buildError(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request,
                                                java.util.List<String> details) {
        ErrorDto errorDto = new ErrorDto();
        errorDto.setStatus("Failure");
        errorDto.setErrorCode(code);
        errorDto.setMessage(message);
        errorDto.setPath(request.getRequestURI());
        errorDto.setTimestamp(Instant.now());
        errorDto.setCorrelationId(MDC.get("correlationId"));
        errorDto.setDetails(details);
        return new ResponseEntity<>(errorDto, status);
    }
}