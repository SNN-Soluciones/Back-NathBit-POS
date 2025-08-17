package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ResumenCajaDiaResponse {
    private LocalDate fecha;
    private Integer totalSesiones;
    private Integer sesionesAbiertas;
    private Integer sesionesCerradas;
    private BigDecimal montoTotalVentas;
    private BigDecimal montoTotalDevoluciones;
    private BigDecimal montoTotalEfectivo;
    private BigDecimal montoTotalTarjeta;
    private BigDecimal montoTotalTransferencia;
    private BigDecimal montoTotalDiferencias;
    private List<ResumenPorTerminal> resumenPorTerminal;
    
    @Data
    @Builder
    public static class ResumenPorTerminal {
        private Long terminalId;
        private String terminalNombre;
        private Integer cantidadSesiones;
        private BigDecimal totalVentas;
        private BigDecimal totalDiferencia;
        private String estado; // ABIERTA, CERRADA
    }
}