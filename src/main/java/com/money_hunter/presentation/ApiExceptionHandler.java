package com.money_hunter.presentation;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
		String message = exception.getReason() == null ? "요청에 실패했어요." : exception.getReason();
		return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleIllegalState(IllegalStateException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Map<String, String> handleValidation(MethodArgumentNotValidException exception) {
		return Map.of("message", "Invalid request.");
	}
}
