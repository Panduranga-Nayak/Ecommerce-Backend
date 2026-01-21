package com.scaler.productcatalogservice.handler;

import com.scaler.ecommerce.common.dtos.ErrorDto;
import com.scaler.productcatalogservice.exception.AccessDeniedException;
import com.scaler.productcatalogservice.exception.CategoryAlreadyExistsException;
import com.scaler.productcatalogservice.exception.CategoryNotFoundException;
import com.scaler.productcatalogservice.exception.DuplicateSkuException;
import com.scaler.productcatalogservice.exception.ProductNotFoundException;
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

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorDto> handleProductNotFound(ProductNotFoundException exception,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorDto> handleCategoryNotFound(CategoryNotFoundException exception,
                                                           HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<ErrorDto> handleDuplicateSku(DuplicateSkuException exception,
                                                       HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "DUPLICATE_SKU", exception.getMessage(), request, null);
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleCategoryExists(CategoryAlreadyExistsException exception,
                                                         HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "CATEGORY_EXISTS", exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException exception,
                                                       HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), request, null);
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