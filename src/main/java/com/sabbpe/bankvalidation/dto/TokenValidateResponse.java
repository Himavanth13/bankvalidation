package com.sabbpe.bankvalidation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenValidateResponse {
    private boolean valid;
}