package com.snnsoluciones.backnathbitpos.dto.auth;

import lombok.Data;

@Data
public class TokenResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn; // Segundos hasta expiración
}