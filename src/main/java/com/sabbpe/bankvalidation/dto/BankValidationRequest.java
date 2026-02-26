package com.sabbpe.bankvalidation.dto;

import lombok.Data;

@Data
public class BankValidationRequest {
	private String entityId;
	private String programId;
	private String requestId;
	private String custName;
	private String custIfsc;
	private String custAcctNo;
	private String trackingRefNo;
	private String txnType;
}
