// CierreCajaResponse.java
package com.snnsoluciones.backnathbitpos.dto.cash;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para cierre de caja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de cierre de caja")
public class CierreCajaResponse {
    
    @Schema(description = "ID del cierre")
    private UUID id;
    
    @Schema(description = "ID de la apertura asociada")
    private UUID aperturaId;
    
    @Schema(description = "ID de la caja")
    private UUID cajaId;
    
    @Schema(description = "Nombre de la caja")
    private String cajaNombre;
    
    @Schema(description = "Usuario que realizó el cierre")
    private String usuario;
    
    @Schema(description = "Fecha y hora de apertura")
    private LocalDateTime fechaApertura;
    
    @Schema(description = "Fecha y hora de cierre")
    private LocalDateTime fechaCierre;
    
    @Schema(description = "Duración del turno en minutos")
    private Long duracionMinutos;
    
    // Montos
    @Schema(description = "Monto inicial de apertura")
    private BigDecimal montoInicial;
    
    @Schema(description = "Total de ventas del turno")
    private BigDecimal montoVentas;
    
    @Schema(description = "Monto esperado (inicial + ventas)")
    private BigDecimal montoEsperado;
    
    @Schema(description = "Monto final contado")
    private BigDecimal montoFinal;
    
    @Schema(description = "Diferencia (final - esperado)")
    private BigDecimal diferencia;
    
    @Schema(description = "Estado del cuadre", example = "CUADRADO, SOBRANTE, FALTANTE")
    private String estado;
    
    // Detalles
    @Schema(description = "Denominaciones finales detalladas")
    private DenominacionesDTO denominaciones;
    
    @Schema(description = "Resumen de ventas por tipo de pago")
    private ResumenVentasDTO resumenVentas;
    
    @Schema(description = "Observaciones del cierre")
    private String observaciones;
    
    @Schema(description = "Mensaje de confirmación")
    private String mensaje;
    
    // Estadísticas del turno
    @Schema(description = "Cantidad de transacciones")
    private Integer cantidadTransacciones;
    
    @Schema(description = "Ticket promedio")
    private BigDecimal ticketPromedio;
    
    /**
     * DTO para resumen de ventas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Resumen de ventas por tipo de pago")
    public static class ResumenVentasDTO {
        
        @Schema(description = "Total en efectivo")
        private BigDecimal efectivo;
        
        @Schema(description = "Total en tarjetas")
        private BigDecimal tarjetas;
        
        @Schema(description = "Total en transferencias")
        private BigDecimal transferencias;
        
        @Schema(description = "Total otros medios")
        private BigDecimal otros;
        
        @Schema(description = "Total general")
        private BigDecimal total;
        
        @Schema(description = "Detalle por tipo de pago")
        private List<DetallePagoDTO> detallePagos;
    }
    
    /**
     * DTO para detalle de pagos
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detalle de un tipo de pago")
    public static class DetallePagoDTO {
        
        @Schema(description = "Tipo de pago", example = "EFECTIVO, VISA, MASTERCARD, SINPE")
        private String tipoPago;
        
        @Schema(description = "Cantidad de transacciones")
        private Integer cantidad;
        
        @Schema(description = "Monto total")
        private BigDecimal monto;
        
        @Schema(description = "Porcentaje del total")
        private BigDecimal porcentaje;
    }
}