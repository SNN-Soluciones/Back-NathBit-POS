// src/main/java/com/snnsoluciones/backnathbitpos/dto/RegistrarPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPagoDTO {
    
    private Long sucursalId;
    private BigDecimal monto;
    private LocalDate fechaPago;
    private String metodoPago;
    private String comprobante;
    private String notas;
}