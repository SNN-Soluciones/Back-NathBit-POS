// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2AbrirSesionResponse.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2AbrirSesionResponse {
    private Long          sesionId;
    private Long          turnoId;       // turno del usuario que abrió
    private String        terminal;
    private String        modoGaveta;
    private BigDecimal    montoInicial;
    private LocalDateTime fechaApertura;
}