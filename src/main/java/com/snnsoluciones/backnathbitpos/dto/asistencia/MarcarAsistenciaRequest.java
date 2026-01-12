package com.snnsoluciones.backnathbitpos.dto.asistencia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request para marcar entrada o salida
 * Endpoint: POST /api/asistencia/marcar
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcarAsistenciaRequest {
    
    /**
     * Tipo de marcación: "ENTRADA" o "SALIDA"
     */
    @NotNull(message = "El tipo es obligatorio")
    @Pattern(regexp = "^(ENTRADA|SALIDA)$", message = "El tipo debe ser ENTRADA o SALIDA")
    private String tipo;
    
    /**
     * Observaciones opcionales
     */
    private String observaciones;
}