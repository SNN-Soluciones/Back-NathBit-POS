// EmpresasResumenResponse.java
package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EmpresasResumenResponse {

    private List<EmpresaCard> empresas;

    @Data
    @Builder
    public static class EmpresaCard {
        private Long          empresaId;
        private String        empresaNombre;
        private boolean       activa;
        private BigDecimal    ventasHoy;
        private BigDecimal    ventasAyer;
        private BigDecimal    porcentajeVsAyer;
        private String        tendencia;        // "up" | "down" | "stable"
        private long          cajasAbiertas;
        private long          facturasHoy;
        private long          sucursales;
        private LocalDateTime ultimaVenta;
    }
}