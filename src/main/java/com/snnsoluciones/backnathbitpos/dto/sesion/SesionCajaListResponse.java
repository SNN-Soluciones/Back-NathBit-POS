package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SesionCajaListResponse {
    private Long id;
    private String terminal;
    private String cajero;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String estado;
    private BigDecimal totalVentas;
    private BigDecimal diferencia;
    private boolean puedeEditarse;
    private boolean puedeCerrarse;
}