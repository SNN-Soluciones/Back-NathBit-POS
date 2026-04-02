// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2CerrarTurnoResponse.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2CerrarTurnoResponse {
    private Long          turnoId;
    private Long          sesionId;
    private boolean       sesionCerrada;   // true si era el último cajero
    private String        mensaje;

    // Ventas reales (calculadas desde facturas)
    private BigDecimal    ventasEfectivo;
    private BigDecimal    ventasTarjeta;
    private BigDecimal    ventasSinpe;
    private BigDecimal    ventasTransferencia;
    private BigDecimal    ventasCredito;

    // Arqueo
    private BigDecimal    montoEsperado;
    private BigDecimal    montoContado;
    private BigDecimal    montoRetirado;
    private BigDecimal    fondoCaja;

    // Diferencias
    private BigDecimal    difEfectivo;
    private BigDecimal    difTarjeta;
    private BigDecimal    difSinpe;
    private BigDecimal    difTransferencia;

    private LocalDateTime fechaFin;
}