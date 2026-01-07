package com.snnsoluciones.backnathbitpos.dto.inventario;

import com.snnsoluciones.backnathbitpos.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para movimientos de inventario (Kardex)
 * Representa una línea en el historial de movimientos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioDTO {

    private Long id;

    // Producto
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private String unidadMedida;

    // Sucursal
    private Long sucursalId;
    private String sucursalNombre;

    // Movimiento
    private TipoMovimiento tipoMovimiento;
    private String tipoMovimientoDescripcion;
    private Boolean esEntrada; // true = entrada, false = salida

    // Cantidades
    private BigDecimal cantidad; // Positivo = entrada, Negativo = salida
    private BigDecimal saldoAnterior;
    private BigDecimal saldoNuevo;

    // Valores monetarios (opcional)
    private BigDecimal precioUnitario;
    private BigDecimal costoTotal;

    // Referencias
    private String documentoReferencia;
    private String observaciones;

    // Usuario y fecha
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaMovimiento;

    // Helper para mostrar cantidad con signo en frontend
    public String getCantidadFormateada() {
        if (cantidad.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + cantidad;
        }
        return cantidad.toString();
    }
}