package com.snnsoluciones.backnathbitpos.dto.movimiento;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para un movimiento de caja
 * Se usa tanto para crear como para listar movimientos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCajaDTO {
    
    private Long id;
    
    /**
     * Tipo de movimiento en formato String (ej: "SALIDA_ARQUEO")
     */
    private String tipoMovimiento;
    
    /**
     * Descripción legible del tipo (ej: "Arqueo de caja")
     */
    private String descripcionTipo;
    
    @DecimalMin(value = "0.01")
    private BigDecimal monto;
    
    /**
     * Concepto o descripción del movimiento
     */
    private String concepto;
    
    /**
     * ID del usuario que autorizó el movimiento
     */
    private Long autorizadoPorId;
    
    /**
     * Nombre del usuario que autorizó (opcional)
     */
    private String nombreAutoriza;
    
    /**
     * Fecha y hora del movimiento
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;
    
    /**
     * Observaciones adicionales
     */
    private String observaciones;
    
    // ===== CAMPOS ADICIONALES SEGÚN EL TIPO =====
    
    /**
     * Nombre del proveedor (solo para SALIDA_PAGO_PROVEEDOR)
     */
    private String nombreProveedor;
    
    /**
     * Motivo del gasto (solo para SALIDA_OTROS)
     */
    private String motivo;
    
    // ===== INDICADORES =====
    
    /**
     * Indica si el movimiento es una entrada
     */
    private boolean esEntrada;
    
    /**
     * Indica si el movimiento es una salida
     */
    private boolean esSalida;
}