package com.snnsoluciones.backnathbitpos.dto.bitacora;

import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para mostrar bitácoras en lista/tabla
 * Información resumida para vista rápida
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaBitacoraListResponse {
    
    private Long id;
    private Long facturaId;
    private String clave;
    private EstadoBitacora estado;
    private Integer intentos;
    private LocalDateTime proximoIntento;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Datos adicionales útiles para la vista
    private String consecutivoFactura;
    private String nombreCliente;
    private String empresaNombre;
    private String sucursalNombre;
    private BigDecimal montoTotal;
    
    // Indicadores visuales
    private boolean puedeReintentar;
    private boolean tieneError;
    private String mensajeError; // Solo el mensaje resumido
}