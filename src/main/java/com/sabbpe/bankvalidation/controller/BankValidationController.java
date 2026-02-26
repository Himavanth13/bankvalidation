package com.sabbpe.bankvalidation.controller;

import com.sabbpe.bankvalidation.dto.BankValidationRequest;
import com.sabbpe.bankvalidation.dto.BankValidationWrapperRequest;
import com.sabbpe.bankvalidation.dto.TokenGenerateRequest;
import com.sabbpe.bankvalidation.dto.TokenGenerateResponse;
import com.sabbpe.bankvalidation.exception.DownstreamServiceException;
import com.sabbpe.bankvalidation.service.BankValidationService;
import com.sabbpe.bankvalidation.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/wrapper/bank")
@RequiredArgsConstructor
public class BankValidationController {

	private final TokenService tokenService;
	private final BankValidationService bankValidationService;

	@PostMapping("/token")
	public ResponseEntity<TokenGenerateResponse> generateToken(@RequestBody @Valid TokenGenerateRequest request) throws Exception {
		log.info(
				"Incoming wrapper token generation request, clientId={}, processor={}, transactionUserId={}, transactionMerchantId={}",
				request.getClientId(),
				request.getProcessor(),
				request.getTransactionUserId(),
				request.getTransactionMerchantId()
		);
		String token = tokenService.generateToken(request);
		return ResponseEntity.ok(new TokenGenerateResponse(token));
	}

	@PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> validateBankAccount(
			@RequestBody BankValidationWrapperRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader
	) {
		log.info("Incoming bank validation request, requestId={}", request.getRequestId());

		BankValidationRequest bankValidationRequest = request.toBankValidationRequest();

		String wrapperToken = resolveWrapperToken(authorizationHeader, request.getToken());
		if (wrapperToken == null || wrapperToken.isBlank()) {
			log.warn("Missing Authorization token for requestId={}", request.getRequestId());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"message\":\"Authorization header or body token is required\"}");
		}

		try {
			String responseBody = bankValidationService.validateBankAccount(bankValidationRequest, wrapperToken);
			return ResponseEntity.ok(responseBody == null ? "{}" : responseBody);
		} catch (IllegalArgumentException ex) {
			log.warn("Token validation failed for requestId={}", request.getRequestId());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"message\":\"" + sanitize(ex.getMessage()) + "\"}");
		} catch (DownstreamServiceException ex) {
			log.error("Downstream bank validation failed for requestId={}", request.getRequestId(), ex);
			return ResponseEntity.status(ex.getStatusCode())
					.contentType(MediaType.APPLICATION_JSON)
					.body(buildDownstreamBody(ex.getResponseBody()));
		} catch (Exception ex) {
			log.error("Bank validation failed for requestId={}", request.getRequestId(), ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"message\":\"Bank validation failed\"}");
		}
	}

	private String extractToken(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return null;
		}

		String headerValue = authorizationHeader.trim();
		if (headerValue.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
			headerValue = headerValue.substring(7).trim();
		}

		if (headerValue.length() >= 2 && headerValue.startsWith("\"") && headerValue.endsWith("\"")) {
			headerValue = headerValue.substring(1, headerValue.length() - 1).trim();
		}

		return headerValue;
	}

	private String resolveWrapperToken(String authorizationHeader, String bodyToken) {
		String headerToken = extractToken(authorizationHeader);
		if (headerToken != null && !headerToken.isBlank()) {
			return headerToken;
		}

		if (bodyToken == null || bodyToken.isBlank()) {
			return null;
		}

		return bodyToken.trim();
	}

	private String sanitize(String value) {
		if (value == null) {
			return "";
		}

		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String buildDownstreamBody(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "{\"message\":\"Bank validation failed with downstream error\"}";
		}

		String trimmed = responseBody.trim();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			return trimmed;
		}

		return "{\"message\":\"" + sanitize(trimmed) + "\"}";
	}
}
