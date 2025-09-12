package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumenDiarioResponse {
    private LocalDate fecha;
    private Long sucursalId;
    private Long cantidadFacturas;
    private Long cantidadAnuladas;
    private BigDecimal totalVentas;
    private Map<String, BigDecimal> totalesPorMedioPago;
}