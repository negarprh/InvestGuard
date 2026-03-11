package com.example.riskanalyzer.controller;

import com.example.riskanalyzer.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        LOGGER.warn("API request failed: {}", exception.getMessage());
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String message = exception.getReason() != null ? exception.getReason() : exception.getMessage();
        return build(status != null ? status : HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        LOGGER.warn("Invalid request payload", exception);
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIo(IOException exception) {
        LOGGER.error("I/O error while contacting upstream service", exception);
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("HTTP 403") || message.toLowerCase().contains("don't have access")) {
            return build(
                    HttpStatus.BAD_GATEWAY,
                    "Finnhub denied historical data access (/stock/candle). Your key is valid but your plan does not include this endpoint."
            );
        }
        if (message.contains("FINNHUB_API_KEY is missing")) {
            return build(HttpStatus.BAD_GATEWAY, "FINNHUB_API_KEY is missing. Add it in .env and restart the app.");
        }
        return build(HttpStatus.BAD_GATEWAY, "Unable to reach Finnhub right now. Cached data will be used when available.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception exception) {
        LOGGER.error("Unexpected server error", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. Please try again.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message == null ? "" : message);
        return new ResponseEntity<>(error, status);
    }
}
