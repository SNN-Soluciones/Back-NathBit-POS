package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request para generar un token de registro de PDV
 * Endpoint: POST /api/admin/dispositivos/generar-token
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarTokenRequest {
    
    /**
     * Nombre del dispositivo a registrar
     * Ejemplo: "Tablet Caja 1", "iPad Bar"
     */
    @NotBlank(message = "El nombre del dispositivo es obligatorio")
    private String nombreDispositivo;
    
    /**
     * ID de la empresa propietaria
     */
    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long empresaId;
    
    /**
     * ID de la sucursal donde operará el dispositivo
     */
    @NotNull(message = "El ID de la sucursal es obligatorio")
    private Long sucursalId;
}