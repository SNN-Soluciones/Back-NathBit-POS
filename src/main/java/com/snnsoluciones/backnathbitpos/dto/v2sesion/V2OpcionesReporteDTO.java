// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2OpcionesReporteDTO.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2OpcionesReporteDTO {

    @Builder.Default private Boolean incluirMovimientos    = false;
    @Builder.Default private Boolean incluirFacturas       = false;
    @Builder.Default private Boolean incluirDenominaciones = false;
    @Builder.Default private Boolean incluirDatafonos      = false;
    @Builder.Default private Boolean incluirPlataformas    = false;
    @Builder.Default private Boolean incluirVentasCredito  = false;
}