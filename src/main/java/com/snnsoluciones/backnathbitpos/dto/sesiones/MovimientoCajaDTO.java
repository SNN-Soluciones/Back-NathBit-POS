package com.snnsoluciones.backnathbitpos.dto.sesiones;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovimientoCajaDTO {
    private Long id;
    private String tipoMovimiento;
    private BigDecimal monto;
    private String concepto;
    private LocalDateTime fechaHora;
    private Long autorizadoPor;
    private String observaciones;
}