package com.sabbpe.bankvalidation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.bankvalidation.dto.BankValidationRequest;
import com.sabbpe.bankvalidation.dto.BankValidationWrapperRequest;
import com.sabbpe.bankvalidation.dto.TokenGenerateRequest;
import com.sabbpe.bankvalidation.dto.TokenGenerateResponse;
import com.sabbpe.bankvalidation.exception.DownstreamServiceException;
import com.sabbpe.bankvalidation.service.BankValidationService;
import com.sabbpe.bankvalidation.service.TokenService;
import com.sabbpe.bankvalidation.service.WrapperCryptoService;
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
	private final WrapperCryptoService wrapperCryptoService;
	private final ObjectMapper objectMapper = new ObjectMapper();

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
		String wrapperToken = resolveWrapperToken(authorizationHeader, request.getToken());
		if (wrapperToken == null || wrapperToken.isBlank()) {
			log.warn("Missing Authorization token");
			return encryptedResponse(HttpStatus.UNAUTHORIZED,
					"{\"message\":\"Authorization header or body token is required\"}");
		}

		if (request.getEncData() == null || request.getEncData().isBlank()) {
			return encryptedResponse(HttpStatus.BAD_REQUEST,
					"{\"message\":\"encData is required\"}");
		}

		try {
			String decryptedPayload = wrapperCryptoService.decryptPayload(request.getEncData());
			BankValidationRequest bankValidationRequest = objectMapper.readValue(decryptedPayload, BankValidationRequest.class);
			log.info("Incoming bank validation request, requestId={}", bankValidationRequest.getRequestId());

			String responseBody = bankValidationService.validateBankAccount(bankValidationRequest, wrapperToken);
			return encryptedResponse(HttpStatus.OK, responseBody == null ? "{}" : responseBody);
		} catch (IllegalArgumentException ex) {
			log.warn("Token validation failed");
			return encryptedResponse(HttpStatus.UNAUTHORIZED,
					"{\"message\":\"" + sanitize(ex.getMessage()) + "\"}");
		} catch (DownstreamServiceException ex) {
			log.error("Downstream bank validation failed", ex);
			return encryptedResponse(HttpStatus.valueOf(ex.getStatusCode().value()),
					buildDownstreamBody(ex.getResponseBody()));
		} catch (Exception ex) {
			log.error("Bank validation failed", ex);
			return encryptedResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"{\"message\":\"Bank validation failed\"}");
		}
	}

	private ResponseEntity<String> encryptedResponse(HttpStatus status, String plainJsonBody) {
		String encrypted = wrapperCryptoService.encryptPayload(plainJsonBody == null ? "{}" : plainJsonBody);
		String body = "{\"encData\":\"" + sanitize(encrypted) + "\"}";
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);
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
