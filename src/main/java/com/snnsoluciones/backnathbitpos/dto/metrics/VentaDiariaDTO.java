package com.snnsoluciones.backnathbitpos.dto.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiariaDTO {
    private LocalDate fecha;
    private BigDecimal ventasMh;
    private BigDecimal ventasInternas;
    private BigDecimal ventasTotales;
    private BigDecimal impuestoTotal;
    private BigDecimal descuentosTotal;
    private Integer cantidadMh;
    private Integer cantidadInternas;
    private Integer cantidadTotal;
}