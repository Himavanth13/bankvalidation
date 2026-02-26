package com.sabbpe.bankvalidation.controller;

import com.sabbpe.bankvalidation.dto.TokenGenerateRequest;
import com.sabbpe.bankvalidation.dto.TokenGenerateResponse;
import com.sabbpe.bankvalidation.dto.TokenValidateRequest;
import com.sabbpe.bankvalidation.dto.TokenValidateResponse;
import com.sabbpe.bankvalidation.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/generate")
    public ResponseEntity<TokenGenerateResponse> generateToken(@RequestBody @Valid TokenGenerateRequest request) throws Exception {
        String token = tokenService.generateToken(request);
        return ResponseEntity.ok(new TokenGenerateResponse(token));
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidateResponse> validateToken(@RequestBody @Valid TokenValidateRequest request) {
        boolean isValid = tokenService.validateToken(request);
        return ResponseEntity.ok(new TokenValidateResponse(isValid));
    }
}