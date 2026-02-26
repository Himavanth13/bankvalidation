package com.sabbpe.bankvalidation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenValidateRequest {
    @NotBlank(message = "Token is required")
    private String token;

    private String clientId;

    private String processor;
}