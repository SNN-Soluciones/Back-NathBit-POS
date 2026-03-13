package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SesionCajaUsuarioDTO {
    private Long id;
    private Long sesionCajaId;
    private Long usuarioId;
    private String usuarioNombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private String estado;
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasTarjeta;
    private BigDecimal ventasTransferencia;
    private BigDecimal ventasOtros;
    private BigDecimal montoEsperado = BigDecimal.ZERO;
    private BigDecimal montoContado;
    private BigDecimal diferencia;
    private String observacionesCierre;
    private LocalDateTime fechaHoraInicioConteo;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTarjeta;
    private BigDecimal diferenciaSinpe;
    private BigDecimal diferenciaTransferencia;
    private BigDecimal montoRetirado;
    private BigDecimal fondoCaja;
    private Long terminalId;
    private String terminalNombre;
}