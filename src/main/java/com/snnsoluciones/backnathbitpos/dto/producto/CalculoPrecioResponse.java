package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CalculoPrecioResponse {
    private BigDecimal precioBase;
    private BigDecimal totalAdicionales;
    private BigDecimal precioFinal;
    private List<DetalleOpcion> detalleOpciones;
    
    @Data
    @Builder
    public static class DetalleOpcion {
        private Long opcionId;
        private String productoNombre;
        private String slotNombre;
        private BigDecimal precioAdicional; // Puede ser negativo
        private Boolean disponibleEnSucursal;
    }
}