
// VentasPorTipoPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class VentasPorTipoPagoDTO {
    
    private String medioPago;        // Código: 01, 02, 03, etc
    private String descripcion;      // Efectivo, Tarjeta, Cheque, etc
    private Integer cantidadDocumentos;
    private BigDecimal montoTotal;
    private BigDecimal porcentaje;   // Porcentaje del total
}

