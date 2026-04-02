// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2MovimientoResponse.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2MovimientoResponse {
    private Long          id;
    private String        tipo;
    private BigDecimal    monto;
    private String        concepto;
    private String        cajeroNombre;
    private LocalDateTime fechaHora;
}