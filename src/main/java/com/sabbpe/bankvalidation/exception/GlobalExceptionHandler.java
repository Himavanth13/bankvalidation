package com.sabbpe.bankvalidation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
		log.error("Unhandled runtime exception", ex);

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		Map<String, Object> body = Map.of(
				"success", false,
				"message", ex.getMessage() != null ? ex.getMessage() : "Internal server error",
				"status", status.value()
		);

		return ResponseEntity.status(status).body(body);
	}
}
