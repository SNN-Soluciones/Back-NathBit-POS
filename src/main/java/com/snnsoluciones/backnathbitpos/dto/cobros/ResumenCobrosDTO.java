// DTO para resumen
package com.snnsoluciones.backnathbitpos.dto.cobros;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ResumenCobrosDTO {
    private BigDecimal totalCobrado;
    private Integer cantidadPagos;
    private Map<String, BigDecimal> porMedioPago;
    private Map<String, BigDecimal> porCajero;
}