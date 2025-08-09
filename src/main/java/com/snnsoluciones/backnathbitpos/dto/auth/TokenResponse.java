package com.snnsoluciones.backnathbitpos.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
    private String token;
    private String refreshToken;
    private String tipo;
    private Long expiraEn;
    private ContextoDTO contexto;
}