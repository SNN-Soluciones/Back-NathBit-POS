package com.snnsoluciones.backnathbitpos.dto.facturainterna;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DescuentoResponse {
    private Long id;
    private String tipoDescuento;
    private String descripcion;
    private BigDecimal porcentaje;
    private BigDecimal monto;
    private String codigoPromocion;
}