package com.sabbpe.bankvalidation.service;

import com.sabbpe.bankvalidation.util.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WrapperCryptoService {

	@Value("${wrapper.payload-aes-key:${wrapper.transaction-aes-key}}")
	private String payloadAesKey;

	@Value("${wrapper.payload-iv:${wrapper.transaction-iv}}")
	private String payloadIv;

	public String decryptPayload(String encryptedPayload) {
		try {
			return AESUtil.decrypt(encryptedPayload, payloadAesKey, payloadIv);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid encrypted payload", ex);
		}
	}

	public String encryptPayload(String plainPayload) {
		try {
			return AESUtil.encrypt(plainPayload, payloadAesKey, payloadIv);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to encrypt response payload", ex);
		}
	}
}