package com.snnsoluciones.backnathbitpos.dto.sesion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionesImpresionCierreDTO {

    @Builder.Default
    private Boolean incluirFacturas = false;

    @Builder.Default
    private Boolean incluirDenominaciones = false;

    @Builder.Default
    private Boolean incluirDatafonos = false;

    @Builder.Default
    private Boolean incluirMovimientos = false;

    @Builder.Default
    private Boolean incluirPlataformas = false;
}