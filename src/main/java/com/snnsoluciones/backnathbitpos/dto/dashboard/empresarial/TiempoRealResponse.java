package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TiempoRealResponse {
    private BigDecimal ventasHoy;
    private BigDecimal ventasAyer;
    private long facturasHoy;
    private long cajasAbiertas;
    private LocalDateTime ultimaActualizacion;
}