// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2BitacoraCajaFilterRequest.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2BitacoraCajaFilterRequest {

    private Long   sucursalId;
    private Long   terminalId;
    private Long   usuarioId;    // cajero que abrió o tuvo turno
    private String estado;       // ABIERTA | CERRADA
    private String modoGaveta;   // COMPARTIDA | INDIVIDUAL

    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;

    @Builder.Default private Integer page      = 0;
    @Builder.Default private Integer size      = 20;
    @Builder.Default private String  sortBy    = "fechaApertura";
    @Builder.Default private String  sortDir   = "DESC";
}