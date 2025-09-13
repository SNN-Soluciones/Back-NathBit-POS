package com.snnsoluciones.backnathbitpos.dto.sesiones;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EstadoCajaDTO {
    private Long sesionId;
    private String estado; // ABIERTA, CERRADA
    private BigDecimal montoInicial;
    private BigDecimal ventasEfectivo;
    private BigDecimal totalVales;
    private BigDecimal montoEsperado;
    private String terminal;
    private LocalDateTime horaApertura;
}