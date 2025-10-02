package com.snnsoluciones.backnathbitpos.dto.sesion;

import com.snnsoluciones.backnathbitpos.dto.sesion.CerrarSesionRequest.DenominacionDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private BigDecimal totalVales;
    private BigDecimal montoEsperado;
    private BigDecimal montoCierre;
    private BigDecimal diferencia;
    private Integer cantidadFacturas;
    private Integer cantidadTiquetes;
    private Integer cantidadNotasCredito;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalTransferencia;
    private String observaciones;

    // opcional: regresar también el desglose que se guardó
    private List<DenominacionDTO> denominaciones;
}