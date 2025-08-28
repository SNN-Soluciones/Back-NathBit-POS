package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para reprocesar factura manualmente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReprocesarFacturaDto {
    private Long bitacoraId;
    private String motivo;
    private Boolean forzar = false; // Para bypass de validaciones
}

