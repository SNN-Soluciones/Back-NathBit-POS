package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TipoPagoResponse {
    private List<TipoPago> tipos;
    private BigDecimal totalGeneral;
    @Data @Builder
    public static class TipoPago {
        private String tipo;
        private BigDecimal total;
        private BigDecimal porcentaje;
    }
}