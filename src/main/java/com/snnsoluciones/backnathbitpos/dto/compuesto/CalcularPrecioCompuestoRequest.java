package com.snnsoluciones.backnathbitpos.dto.compuesto;

import com.snnsoluciones.backnathbitpos.dto.producto.SlotSeleccionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para calcular precio de producto compuesto
 * Incluye todas las selecciones del usuario con cantidades
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcularPrecioCompuestoRequest {

    /**
     * ID del producto compuesto
     */
    private Long productoId;

    /**
     * ID de la sucursal (para validar stock)
     */
    private Long sucursalId;

    /**
     * ID de la configuración seleccionada (si aplica)
     */
    private Long configuracionId;

    /**
     * Selecciones de todos los slots
     */
    private List<SlotSeleccionDTO> selecciones;
}

/**
 * Response con el desglose de precio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CalcularPrecioCompuestoResponse {

    private BigDecimal precioBase;
    private BigDecimal precioOpciones;
    private BigDecimal precioTotal;
    
    /**
     * Desglose por cada slot
     */
    private List<DetalleSlot> detalleSlots;

    /**
     * Resumen legible para mostrar en ticket
     */
    private String resumen;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleSlot {
        private String slotNombre;
        private List<DetalleOpcion> opciones;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleOpcion {
        private String nombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}