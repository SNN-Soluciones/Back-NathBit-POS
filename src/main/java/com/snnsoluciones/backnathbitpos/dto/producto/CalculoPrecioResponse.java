package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CalculoPrecioResponse {
    private BigDecimal precioBase;
    private BigDecimal totalAdicionales;
    private BigDecimal precioFinal;
    private List<DetalleOpcion> detalleOpciones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleOpcion {
        private Long opcionId;
        private String productoNombre;
        private String slotNombre;
        private Integer cantidad;              // ← NUEVO
        private BigDecimal precioUnitario;     // ← NUEVO (precio de 1 unidad)
        private BigDecimal precioAdicional;    // ← Ahora es el subtotal (unitario × cantidad)
        private Boolean disponibleEnSucursal;
    }
}