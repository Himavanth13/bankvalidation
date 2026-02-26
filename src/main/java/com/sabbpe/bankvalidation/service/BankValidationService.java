package com.sabbpe.bankvalidation.service;

import com.sabbpe.bankvalidation.dto.BankValidationRequest;
import com.sabbpe.bankvalidation.entity.BankValidationAudit;
import com.sabbpe.bankvalidation.exception.DownstreamServiceException;
import com.sabbpe.bankvalidation.repositories.BankValidationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankValidationService {

	private static final String STATUS_SUCCESS = "SUCCESS";
	private static final String STATUS_FAILED = "FAILED";
	private static final String STATUS_ERROR = "ERROR";
	private static final String STATUS_TOKEN_INVALID = "TOKEN_INVALID";

	private final RestTemplate restTemplate;
	private final TokenService tokenService;
	private final BankValidationAuditRepository bankValidationAuditRepository;

	@Value("${trusthub.base-url}")
	private String trustHubBaseUrl;

	@Value("${trusthub.api-key}")
	private String trustHubApiKey;

	public String validateBankAccount(BankValidationRequest request, String wrapperToken) {
		if (request == null) {
			throw new IllegalArgumentException("Bank validation request is required");
		}

		String requestId = request.getRequestId();
		BankValidationAudit audit = initializeAudit(request);
		Instant startTime = Instant.now();

		log.info("Bank validation started, requestId={}, endpoint={}", requestId, trustHubBaseUrl);
		log.debug("Bank validation payload (masked), requestId={}, payload={}", requestId, audit.getRequestPayload());

		validateWrapperTokenOrThrow(wrapperToken, requestId, audit);

		try {
			ResponseEntity<String> response = callTrustHub(request);
			String responsePayload = safeValue(response.getBody());
			String status = response.getStatusCode().is2xxSuccessful() ? STATUS_SUCCESS : STATUS_FAILED;

			persistAudit(audit, status, responsePayload);

			long durationMs = Duration.between(startTime, Instant.now()).toMillis();
			log.info(
					"Bank validation completed, requestId={}, statusCode={}, durationMs={}",
					requestId,
					response.getStatusCode().value(),
					durationMs
			);
			log.debug("TrustHub response body, requestId={}, payload={}", requestId, truncate(responsePayload));

			return responsePayload;
		} catch (HttpStatusCodeException ex) {
			String responsePayload = safeValue(ex.getResponseBodyAsString());
			persistAudit(audit, STATUS_FAILED, responsePayload);

			log.error(
					"TrustHub returned error response, requestId={}, statusCode={}, response={} ",
					requestId,
					ex.getStatusCode().value(),
					truncate(responsePayload),
					ex
			);

			throw new DownstreamServiceException(
					"Bank validation failed with downstream error",
					ex.getStatusCode(),
					responsePayload,
					ex
			);
		} catch (RestClientException ex) {
			persistAudit(audit, STATUS_ERROR, safeValue(ex.getMessage()));
			log.error("TrustHub call failed, requestId={}, message={}", requestId, ex.getMessage(), ex);
			throw new RuntimeException("Failed to validate bank account", ex);
		}
	}

	private BankValidationAudit initializeAudit(BankValidationRequest request) {
		BankValidationAudit audit = new BankValidationAudit();
		audit.setRequestId(request.getRequestId());
		audit.setRequestPayload(buildAuditPayload(request));
		return audit;
	}

	private void validateWrapperTokenOrThrow(String wrapperToken, String requestId, BankValidationAudit audit) {
		if (tokenService.validateToken(wrapperToken)) {
			return;
		}

		String message = "Invalid or expired wrapper token";
		persistAudit(audit, STATUS_TOKEN_INVALID, message);
		log.warn("Wrapper token validation failed, requestId={}", requestId);
		throw new IllegalArgumentException(message);
	}

	private ResponseEntity<String> callTrustHub(BankValidationRequest request) {
		HttpEntity<BankValidationRequest> entity = new HttpEntity<>(request, buildHeaders());
		return restTemplate.exchange(trustHubBaseUrl, HttpMethod.POST, entity, String.class);
	}

	private HttpHeaders buildHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-api-key", trustHubApiKey);
		return headers;
	}

	private void persistAudit(BankValidationAudit audit, String status, String responsePayload) {
		audit.setStatus(status);
		audit.setResponsePayload(truncate(responsePayload));
		bankValidationAuditRepository.save(audit);
	}

	private String buildAuditPayload(BankValidationRequest request) {
		if (request == null) {
			return null;
		}

		return "{"
				+ "\"entityId\":\"" + safeValue(request.getEntityId()) + "\"," 
				+ "\"programId\":\"" + safeValue(request.getProgramId()) + "\"," 
				+ "\"requestId\":\"" + safeValue(request.getRequestId()) + "\"," 
				+ "\"custName\":\"" + safeValue(request.getCustName()) + "\"," 
				+ "\"custIfsc\":\"" + safeValue(request.getCustIfsc()) + "\"," 
				+ "\"custAcctNo\":\"" + maskAccountNumber(request.getCustAcctNo()) + "\"," 
				+ "\"trackingRefNo\":\"" + safeValue(request.getTrackingRefNo()) + "\"," 
				+ "\"txnType\":\"" + safeValue(request.getTxnType()) + "\""
				+ "}";
	}

	private String maskAccountNumber(String accountNumber) {
		if (accountNumber == null || accountNumber.length() < 4) {
			return "****";
		}

		String suffix = accountNumber.substring(accountNumber.length() - 4);
		return "****" + suffix;
	}

	private String truncate(String value) {
		if (value == null) {
			return null;
		}

		int maxLength = 4000;
		if (value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
