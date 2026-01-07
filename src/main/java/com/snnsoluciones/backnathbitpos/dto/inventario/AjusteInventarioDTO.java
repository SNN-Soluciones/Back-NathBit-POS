package com.snnsoluciones.backnathbitpos.dto.inventario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para ajustar inventario manualmente
 * Usado por ADMIN/SUPER_ADMIN para entradas/salidas manuales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusteInventarioDTO {

    @NotNull(message = "El producto es requerido")
    private Long productoId;

    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;

    /**
     * Tipo de ajuste:
     * - ENTRADA_COMPRA: Compra a proveedor
     * - ENTRADA_AJUSTE: Ajuste positivo (sobrante)
     * - ENTRADA_DEVOLUCION: Devolución de cliente
     * - ENTRADA_INICIAL: Carga inicial de inventario
     * - SALIDA_AJUSTE: Ajuste negativo (merma, robo, etc)
     * - SALIDA_MERMA: Pérdida específica (vencimiento, daño)
     * - SALIDA_CONSUMO: Consumo interno
     */
    @NotNull(message = "El tipo de movimiento es requerido")
    private String tipoMovimiento; // Viene del enum TipoMovimiento

    /**
     * Cantidad a ajustar (siempre positiva, el signo lo define tipoMovimiento)
     */
    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    /**
     * Motivo/Observaciones del ajuste
     * Ej: "Inventario inicial", "Merma por vencimiento", "Robo", etc.
     */
    @NotNull(message = "Las observaciones son requeridas")
    private String observaciones;

    /**
     * Precio unitario (opcional, útil para entradas)
     */
    private BigDecimal precioUnitario;

    /**
     * Documento de referencia (opcional)
     * Ej: "COMPRA-123", "AJUSTE-2024-001"
     */
    private String documentoReferencia;
}