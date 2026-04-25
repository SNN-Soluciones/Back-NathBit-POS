package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class HorasPicoResponse {
    private List<HoraData> horas;
    private int horaPico;
    private double promedioFacturasHora;
    @Data @Builder
    public static class HoraData {
        private int hora;
        private BigDecimal total;
        private long facturas;
    }
}