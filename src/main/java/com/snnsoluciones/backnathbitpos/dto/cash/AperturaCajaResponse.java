// AperturaCajaResponse.java
package com.snnsoluciones.backnathbitpos.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para apertura de caja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de apertura de caja")
public class AperturaCajaResponse {
    
    @Schema(description = "ID de la apertura")
    private UUID id;
    
    @Schema(description = "ID de la caja")
    private UUID cajaId;
    
    @Schema(description = "Nombre de la caja")
    private String cajaNombre;
    
    @Schema(description = "Usuario que realizó la apertura")
    private String usuario;
    
    @Schema(description = "Fecha y hora de apertura")
    private LocalDateTime fechaHora;
    
    @Schema(description = "Monto inicial de apertura")
    private BigDecimal montoInicial;
    
    @Schema(description = "Denominaciones detalladas")
    private DenominacionesDTO denominaciones;
    
    @Schema(description = "Estado de la apertura", example = "ACTIVO")
    private String estado;
    
    @Schema(description = "Observaciones")
    private String observaciones;
    
    @Schema(description = "Mensaje de confirmación")
    private String mensaje;
    
    @Schema(description = "Información de la sucursal")
    private String sucursalNombre;
    
    @Schema(description = "Número de turno/sesión")
    private Integer numeroTurno;
}