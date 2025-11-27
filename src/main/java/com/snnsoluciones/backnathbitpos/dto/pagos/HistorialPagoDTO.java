// src/main/java/com/snnsoluciones/backnathbitpos/dto/HistorialPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPagoDTO {
    
    private Long id;
    private Long sucursalId;
    private String nombreSucursal;
    private LocalDate fechaPago;
    private BigDecimal monto;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String metodoPago;
    private String comprobante;
    private String notas;
    private String registradoPor; // Nombre del usuario
}