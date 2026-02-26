package com.sabbpe.bankvalidation.dto;

import lombok.Data;

@Data
public class BankValidationWrapperRequest {
	private String token;
	private String entityId;
	private String programId;
	private String requestId;
	private String custName;
	private String custIfsc;
	private String custAcctNo;
	private String trackingRefNo;
	private String txnType;

	public BankValidationRequest toBankValidationRequest() {
		BankValidationRequest request = new BankValidationRequest();
		request.setEntityId(entityId);
		request.setProgramId(programId);
		request.setRequestId(requestId);
		request.setCustName(custName);
		request.setCustIfsc(custIfsc);
		request.setCustAcctNo(custAcctNo);
		request.setTrackingRefNo(trackingRefNo);
		request.setTxnType(txnType);
		return request;
	}
}
