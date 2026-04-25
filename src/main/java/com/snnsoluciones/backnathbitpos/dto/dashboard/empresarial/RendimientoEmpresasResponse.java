package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data @Builder
public class RendimientoEmpresasResponse {
    private List<EmpresaRendimiento> empresas;
    @Data @Builder @Setter
    public static class EmpresaRendimiento {
        private Long empresaId;
        private String empresaNombre;
        private BigDecimal totalVentas;
        private BigDecimal totalAnterior;
        private BigDecimal porcentajeCambio;
        private String tendencia;
        private BigDecimal ticketPromedio;
        private long totalFacturas;
        private long sucursalesActivas;
        private String topProducto;
        private String topTipoPago;
        private BigDecimal participacion;
    }
}