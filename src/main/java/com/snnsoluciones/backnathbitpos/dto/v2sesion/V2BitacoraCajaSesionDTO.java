// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2BitacoraCajaSesionDTO.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2BitacoraCajaSesionDTO {

    private Long          sesionId;
    private String        terminal;
    private String        sucursal;
    private String        usuarioApertura;
    private String        modoGaveta;
    private String        estado;

    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    private BigDecimal    montoInicial;
    private BigDecimal    totalEfectivo;
    private BigDecimal    totalTarjeta;
    private BigDecimal    totalSinpe;
    private BigDecimal    totalTransferencia;
    private BigDecimal    totalVentas;

    private int           cantidadTurnos;
    private List<TurnoResumenDTO> turnos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnoResumenDTO {
        private Long          turnoId;
        private String        cajero;
        private String        estado;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private BigDecimal    totalVentas;
        private BigDecimal    fondoInicio;
        private BigDecimal    fondoCaja;
    }
}