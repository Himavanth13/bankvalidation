package com.sabbpe.bankvalidation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TokenGenerateRequest {

    @NotBlank(message = "Transaction user ID is required")
    @JsonProperty("transaction_userid")
    private String transactionUserId;

    @NotBlank(message = "Transaction merchant ID is required")
    @JsonProperty("transaction_merchantid")
    private String transactionMerchantId;

    @NotBlank(message = "Client ID is required")
    @JsonProperty("client_Id")
    private String clientId;

    @NotBlank(message = "Transaction timestamp is required")
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
            message = "Timestamp must be in format: yyyy-MM-dd HH:mm:ss"
    )
    @JsonProperty("transaction_timestamp")
    private String transactionTimestamp;

    @NotBlank(message = "Processor is required")
    private String processor;
}