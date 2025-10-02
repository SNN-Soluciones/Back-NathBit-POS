// CerrarSesionRequest.java
package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CerrarSesionRequest {
    @NotNull
    private BigDecimal montoCierre;

    @Size(max = 500)
    private String observaciones;

    @NotNull
    @Size(min = 1)
    private List<DenominacionDTO> denominaciones;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DenominacionDTO {
        @NotNull private BigDecimal valor;     // ej: 1000, 2000, 5000, etc
        @NotNull private Integer cantidad;     // ej: 3 billetes de 2000
    }
}