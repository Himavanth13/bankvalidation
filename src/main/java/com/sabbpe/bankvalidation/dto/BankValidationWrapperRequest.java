package com.sabbpe.bankvalidation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankValidationWrapperRequest {
	private String token;

	@JsonProperty("encData")
	@JsonAlias({"encryptedData"})
	private String encData;

	public BankValidationRequest toBankValidationRequest() {
		throw new UnsupportedOperationException("Use decrypted payload to build BankValidationRequest");
	}
}
