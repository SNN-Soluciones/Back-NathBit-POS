
// VentasPorTipoPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class VentasPorTipoPagoDTO {
    
    private String medioPago;        // Código: 01, 02, 03, etc
    private String descripcion;      // Efectivo, Tarjeta, Cheque, etc
    private Integer cantidadDocumentos;
    private BigDecimal montoTotal;
    private BigDecimal porcentaje;   // Porcentaje del total
}

