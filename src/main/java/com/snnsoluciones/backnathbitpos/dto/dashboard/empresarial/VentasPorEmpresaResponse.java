package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
 
@Data @Builder
public class VentasPorEmpresaResponse {
    private List<EmpresaVentas> empresas;
 
    @Data @Builder
    public static class EmpresaVentas {
        private Long       empresaId;
        private String     empresaNombre;
        private BigDecimal totalActual;
        private BigDecimal totalAnterior;
        private BigDecimal porcentajeCambio;
        private String     tendencia;
        private long       totalFacturas;
    }
}