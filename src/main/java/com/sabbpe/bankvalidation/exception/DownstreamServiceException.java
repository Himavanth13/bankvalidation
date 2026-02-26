package com.sabbpe.bankvalidation.exception;

import org.springframework.http.HttpStatusCode;

public class DownstreamServiceException extends RuntimeException {

	private final HttpStatusCode statusCode;
	private final String responseBody;

	public DownstreamServiceException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}
}
