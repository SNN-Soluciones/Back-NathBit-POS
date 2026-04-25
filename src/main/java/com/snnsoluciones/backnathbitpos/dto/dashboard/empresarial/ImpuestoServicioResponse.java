package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Data @Builder
public class ImpuestoServicioResponse {
    private ResumenServicio resumen;
    private List<DesglosePorEmpresa> desglosePorEmpresa;
    private List<DesglosePorSucursal> desglosePorSucursal;
    @Data @Builder
    public static class ResumenServicio {
        private BigDecimal totalServicio;
        private long totalFacturasConServicio;
        private BigDecimal promedioServicioFactura;
        private long totalFacturas;
        private BigDecimal porcentajeCobertura;
        private String periodoDesde;
        private String periodoHasta;
    }
    @Data @Builder @Setter
    public static class DesglosePorEmpresa {
        private Long empresaId;
        private String empresaNombre;
        private BigDecimal totalServicio;
        private long facturasConServicio;
        private BigDecimal porcentaje;
    }
    @Data @Builder @Setter
    public static class DesglosePorSucursal {
        private Long sucursalId;
        private String sucursalNombre;
        private String empresaNombre;
        private BigDecimal totalServicio;
        private long facturasConServicio;
        private BigDecimal porcentaje;
    }
}