package com.snnsoluciones.backnathbitpos.dto.factura;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para dashboard/resumen de estado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenBitacoraDto {
    private Long totalFacturas;
    private Long pendientes;
    private Long procesando;
    private Long aceptadas;
    private Long rechazadas;
    private Long conError;
    
    // Stats adicionales
    private Double tasaExito; // % de aceptadas
    private Double tiempoPromedioSegundos;
    private LocalDateTime ultimaProcesada;
}
