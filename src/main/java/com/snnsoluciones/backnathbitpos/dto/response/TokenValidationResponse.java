package com.snnsoluciones.backnathbitpos.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private String username;
    private List<String> authorities;
}