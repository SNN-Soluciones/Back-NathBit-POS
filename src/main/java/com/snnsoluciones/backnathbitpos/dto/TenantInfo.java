package com.snnsoluciones.backnathbitpos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantInfo {
    private String tenantId;
    private String tenantNombre;
    private String tenantTipo;
    private String rol;
}