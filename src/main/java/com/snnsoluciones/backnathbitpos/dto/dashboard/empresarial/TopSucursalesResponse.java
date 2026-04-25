package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TopSucursalesResponse {
    private List<SucursalTop> sucursales;
    @Data @Builder
    public static class SucursalTop {
        private Long sucursalId;
        private String sucursalNombre;
        private String empresaNombre;
        private BigDecimal total;
        private BigDecimal porcentaje;
        private long totalFacturas;
        private BigDecimal ticketPromedio;
    }
}