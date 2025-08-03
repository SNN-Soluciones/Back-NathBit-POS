package com.snnsoluciones.backnathbitpos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSelectionResponse {
    private String accessToken;
    private String tenantId;
    private String tenantNombre;
    private String tenantTipo;
    private String rol;
    private boolean esPropietario;
}