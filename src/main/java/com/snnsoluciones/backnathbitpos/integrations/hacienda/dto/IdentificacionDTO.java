package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentificacionDTO {
    private String tipoIdentificacion;      // "01" | "02" | ...
    private String numeroIdentificacion;    // sin guiones
}

