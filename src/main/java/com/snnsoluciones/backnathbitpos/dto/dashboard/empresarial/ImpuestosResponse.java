package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data @Builder
public class ImpuestosResponse {
    private ResumenIVA resumen;
    private List<DesglosePorTarifa> desglosePorTarifa;
    private List<DesglosePorEmpresa> desglosePorEmpresa;
    @Data @Builder
    public static class ResumenIVA {
        private BigDecimal totalIVA;
        private BigDecimal totalBaseImponible;
        private BigDecimal totalExonerado;
        private String periodoDesde;
        private String periodoHasta;
    }
    @Data @Builder
    public static class DesglosePorTarifa {
        private String tarifa;
        private String codigoTarifa;
        private BigDecimal montoIVA;
        private BigDecimal baseImponible;
        private BigDecimal porcentaje;
    }
    @Data @Builder @Setter
    public static class DesglosePorEmpresa {
        private Long empresaId;
        private String empresaNombre;
        private BigDecimal totalIVA;
        private BigDecimal baseImponible;
        private BigDecimal porcentaje;
    }
}