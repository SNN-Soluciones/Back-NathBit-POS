package com.snnsoluciones.backnathbitpos.dto.sesion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CierreCajaResponse {
    private Long sesionId;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal totalVentas;
    private BigDecimal totalDevoluciones;
    private BigDecimal montoEsperado;
    private BigDecimal montoCierre;
    private BigDecimal diferencia;
    private Integer cantidadFacturas;
    private Integer cantidadTiquetes;
    private Integer cantidadNotasCredito;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
}