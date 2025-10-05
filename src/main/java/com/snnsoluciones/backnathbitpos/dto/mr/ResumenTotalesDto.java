package com.snnsoluciones.backnathbitpos.dto.mr;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResumenTotalesDto {
    private BigDecimal totalVentaNeta;
    private BigDecimal totalImpuesto;
    private BigDecimal totalComprobante;
}