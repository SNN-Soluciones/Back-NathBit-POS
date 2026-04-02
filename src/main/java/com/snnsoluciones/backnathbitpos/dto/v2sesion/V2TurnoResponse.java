// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2TurnoResponse.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2TurnoResponse {
    private Long          turnoId;
    private Long          sesionId;
    private Long          usuarioId;
    private String        cajeroNombre;
    private String        estado;
    private BigDecimal    fondoInicio;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String        terminal;
    private String        modoGaveta;
}