package com.snnsoluciones.backnathbitpos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantSelectionRequest {
    @NotBlank
    private String tenantId;
}