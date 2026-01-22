package com.scaler.cartservice.handler;

import com.scaler.cartservice.exception.CartNotFoundException;
import com.scaler.cartservice.exception.InvalidCartItemException;
import com.scaler.cartservice.exception.OrderCreationException;
import com.scaler.cartservice.exception.ProductUnavailableException;
import com.scaler.ecommerce.common.dtos.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorDto> handleCartNotFound(CartNotFoundException exception,
                                                       HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCartItemException.class)
    public ResponseEntity<ErrorDto> handleInvalidItem(InvalidCartItemException exception,
                                                      HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_CART_ITEM", exception.getMessage(), request, null);
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ErrorDto> handleProductUnavailable(ProductUnavailableException exception,
                                                             HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE", exception.getMessage(), request, null);
    }

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ErrorDto> handleOrderCreation(OrderCreationException exception,
                                                        HttpServletRequest request) {
        return buildError(HttpStatus.BAD_GATEWAY, "ORDER_CREATION_FAILED", exception.getMessage(), request, null);
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
        LOGGER.error("Unhandled exception", exception);
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