package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TopProductosResponse {
    private List<Producto> productos;
    @Data @Builder
    public static class Producto {
        private String productoNombre;
        private long cantidadVendida;
        private BigDecimal totalVentas;
        private BigDecimal porcentaje;
    }
}