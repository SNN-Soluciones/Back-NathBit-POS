package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerrarTurnoResponse {

    private Long turnoId;
    private Long sesionCajaId;
    private String estado;

    // Montos esperados (calculados de facturas)
    private BigDecimal montoEsperadoEfectivo;
    private BigDecimal montoEsperadoTarjeta;
    private BigDecimal montoEsperadoTransferencia;
    private BigDecimal montoEsperadoSinpe;

    // Montos declarados por el cajero
    private BigDecimal montoContado;
    private BigDecimal totalEfectivoDeclarado;
    private BigDecimal totalTarjetaDeclarado;
    private BigDecimal totalTransferenciaDeclarado;
    private BigDecimal totalSinpeDeclarado;

    // Diferencias (declarado - esperado)
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTarjeta;
    private BigDecimal diferenciaTransferencia;
    private BigDecimal diferenciaSinpe;

    // Retiro y fondo
    private BigDecimal montoRetirado;
    private BigDecimal fondoCaja;

    private LocalDateTime fechaHoraFin;

    // Flag clave: indica al frontend que debe confirmar el cierre de la sesión maestra
    private boolean esSesionCerrada;
    private String mensajeCierre;
}