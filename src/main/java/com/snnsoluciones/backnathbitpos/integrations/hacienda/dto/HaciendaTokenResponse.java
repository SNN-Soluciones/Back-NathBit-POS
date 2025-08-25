package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HaciendaTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private long expiresIn; // segundos (≈300)

    @JsonProperty("token_type")
    private String tokenType; // "bearer"
}