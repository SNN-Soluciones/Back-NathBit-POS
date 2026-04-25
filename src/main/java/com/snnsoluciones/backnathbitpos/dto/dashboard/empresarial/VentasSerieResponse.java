package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
 
@Data @Builder
public class VentasSerieResponse {
    private List<SeriePorEmpresa> series;
    private String agrupadoPor;
 
    @Data @Builder
    public static class SeriePorEmpresa {
        private Long   empresaId;
        private String empresaNombre;
        private List<PuntoFecha> puntos;
    }
 
    @Data @Builder
    public static class PuntoFecha {
        private String     fecha;
        private BigDecimal total;
    }
}