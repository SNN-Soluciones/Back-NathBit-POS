package com.snnsoluciones.backnathbitpos.dto.inventario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con información de inventario actual
 * Incluye alertas de stock bajo y valor monetario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventarioActualDTO {

    private Long id;

    // Producto
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private String unidadMedida;
    private String tipoInventario;

    // Sucursal
    private Long sucursalId;
    private String sucursalNombre;

    // Cantidades
    private BigDecimal cantidadActual;
    private BigDecimal cantidadMinima;
    private BigDecimal cantidadBloqueada;
    private BigDecimal cantidadDisponible; // cantidadActual - cantidadBloqueada

    // Estado
    private Boolean bajominimo; // cantidadActual < cantidadMinima
    private Boolean agotado; // cantidadDisponible <= 0
    private String estadoStock; // "OK", "BAJO", "AGOTADO", "CRITICO"

    // Valores (opcional)
    private BigDecimal precioCompra;
    private BigDecimal valorInventario; // cantidadActual * precioCompra

    // Fechas
    private LocalDateTime ultimaActualizacion;
    private LocalDateTime ultimaEntrada;
    private LocalDateTime ultimaSalida;

    /**
     * Calcula el estado del stock basado en cantidad actual y mínima
     */
    public String calcularEstadoStock() {
        if (cantidadDisponible.compareTo(BigDecimal.ZERO) <= 0) {
            return "AGOTADO";
        }
        if (cantidadActual.compareTo(cantidadMinima) < 0) {
            BigDecimal diferencia = cantidadMinima.subtract(cantidadActual);
            BigDecimal porcentajeFaltante = diferencia
                .divide(cantidadMinima, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
            
            if (porcentajeFaltante.compareTo(new BigDecimal("50")) >= 0) {
                return "CRITICO"; // Falta más del 50% para llegar al mínimo
            }
            return "BAJO";
        }
        return "OK";
    }
}