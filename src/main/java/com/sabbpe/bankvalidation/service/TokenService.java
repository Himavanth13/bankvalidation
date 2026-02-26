package com.sabbpe.bankvalidation.service;

import com.sabbpe.bankvalidation.dto.TokenGenerateRequest;
import com.sabbpe.bankvalidation.dto.TokenValidateRequest;
import com.sabbpe.bankvalidation.entity.TransactionToken;
import com.sabbpe.bankvalidation.repositories.TransactionTokenRepository;
import com.sabbpe.bankvalidation.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

	private final TransactionTokenRepository transactionTokenRepository;

	@Value("${wrapper.transaction-password}")
	private String transactionPassword;

	@Value("${wrapper.transaction-aes-key}")
	private String transactionAesKey;

	@Value("${wrapper.transaction-iv}")
	private String transactionIv;

	private static final DateTimeFormatter TS_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public String generateToken(TokenGenerateRequest request) throws Exception {
		String processor = request.getProcessor();

		if (processor == null || processor.isBlank()) {
			throw new IllegalArgumentException("Processor is required");
		}

		LocalDateTime ldt = LocalDateTime.parse(request.getTransactionTimestamp(), TS_FORMATTER);
		String normalizedTs = ldt.format(TS_FORMATTER);

		Optional<TransactionToken> existingTxn = transactionTokenRepository
				.findLatestByClientIdAndTransactionTimestampAndProcessor(
						request.getClientId(),
						ldt,
						processor.toUpperCase()
				);

		if (existingTxn.isPresent()) {
			TransactionToken existing = existingTxn.get();
			long minutesPassed = Duration.between(ldt, LocalDateTime.now()).toMinutes();

			if (minutesPassed < 15) {
				log.warn("Token already exists | Returning existing token");
				return existing.getTransactionToken();
			}

			log.info("Existing token expired, generating new one");
		}

		String raw = request.getTransactionUserId()
				+ request.getTransactionMerchantId()
				+ transactionPassword
				+ normalizedTs
				+ processor.toUpperCase();

		String encryptedToken = AESUtil.encrypt(raw, transactionAesKey, transactionIv);

		String internalTxnRef = "TXN"
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		TransactionToken masterTxn = TransactionToken.builder()
				.orderReference(internalTxnRef)
				.transactionToken(encryptedToken)
				.transactionTimestamp(ldt)
				.transactionUserId(request.getTransactionUserId())
				.transactionMerchantId(request.getTransactionMerchantId())
				.clientId(request.getClientId())
				.processor(processor.toUpperCase())
				.build();

		transactionTokenRepository.save(masterTxn);

		log.info("Token generated | Client: {} | Processor: {} | Ref: {}",
				request.getClientId(), processor.toUpperCase(), internalTxnRef);

		return encryptedToken;
	}

	public boolean validateToken(TokenValidateRequest request) {
		return validateToken(request.getToken(), request.getClientId(), request.getProcessor());
	}

	public boolean validateToken(String token) {
		return validateToken(token, null, null);
	}

	public boolean validateToken(String token, String clientId, String processor) {
		try {
			if (token == null || token.isBlank()) {
				return false;
			}

			TransactionToken masterTxn = transactionTokenRepository
					.findByTransactionToken(token)
					.orElseThrow(() -> new IllegalArgumentException("Invalid or unknown transaction token"));

			if (clientId != null && !clientId.isBlank() && !masterTxn.getClientId().equals(clientId)) {
				throw new IllegalArgumentException("Token does not belong to this client");
			}

			if (processor != null && !processor.isBlank() && !masterTxn.getProcessor().equalsIgnoreCase(processor)) {
				throw new IllegalArgumentException("Token processor mismatch");
			}

			LocalDateTime ts = masterTxn.getTransactionTimestamp();
			if (ts == null) {
				throw new IllegalStateException("Missing merchant transaction timestamp");
			}

			long minutesPassed = Duration.between(ts, LocalDateTime.now()).toMinutes();
			if (minutesPassed >= 15) {
				throw new IllegalArgumentException("Transaction token expired");
			}

			String expectedRaw = masterTxn.getTransactionUserId()
					+ masterTxn.getTransactionMerchantId()
					+ transactionPassword
					+ ts.format(TS_FORMATTER)
					+ masterTxn.getProcessor();

			String decryptedRaw = AESUtil.decrypt(token, transactionAesKey, transactionIv);
			if (!expectedRaw.equals(decryptedRaw)) {
				throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
			}

			log.info("Transaction token validated for client_id={} merchantId={}",
					masterTxn.getClientId(), masterTxn.getTransactionMerchantId());
			return true;
		} catch (Exception ex) {
			log.warn("Token validation failed: {}", ex.getMessage());
			return false;
		}
	}
}
