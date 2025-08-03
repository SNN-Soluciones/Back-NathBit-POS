package com.snnsoluciones.backnathbitpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantSelectionRequest {
    @NotBlank(message = "El ID del tenant es requerido")
    private String tenantId;
}