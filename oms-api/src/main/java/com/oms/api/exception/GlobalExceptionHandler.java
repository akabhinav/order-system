package com.oms.api.exception;

import com.oms.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid Order State Transition");
        problem.setType(URI.create("urn:oms:error:invalid-order-state"));
        problem.setProperty("currentStatus", ex.currentStatus().name());
        problem.setProperty("attemptedStatus", ex.attemptedStatus().name());
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("urn:oms:error:order-not-found"));
        problem.setProperty("orderId", ex.orderId().value().toString());
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Insufficient Stock");
        problem.setType(URI.create("urn:oms:error:insufficient-stock"));
        problem.setProperty("productId", ex.productId().value().toString());
        problem.setProperty("requested", ex.requested());
        problem.setProperty("available", ex.available());
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ProblemDetail handleDuplicateOrder(DuplicateOrderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Order");
        problem.setType(URI.create("urn:oms:error:duplicate-order"));
        problem.setProperty("idempotencyKey", ex.idempotencyKey().toString());
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("urn:oms:error:validation"));

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        problem.setProperty("fieldErrors", fieldErrors);
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Missing Required Header");
        problem.setType(URI.create("urn:oms:error:missing-header"));
        problem.setProperty("headerName", ex.getHeaderName());
        addRequestId(problem);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        String requestId = MDC.get("requestId");
        log.error("Unhandled exception [requestId={}]", requestId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Reference: " + requestId);
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:oms:error:internal"));
        addRequestId(problem);
        return problem;
    }

    private void addRequestId(ProblemDetail problem) {
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
    }
}
