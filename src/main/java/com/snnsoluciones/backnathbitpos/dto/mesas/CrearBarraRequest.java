// dto/mesas/CrearBarraRequest.java
package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.TipoFormaBarra;
import jakarta.validation.constraints.*;

public record CrearBarraRequest(
    @NotBlank @Size(max = 20) String codigo,
    @Size(max = 60) String nombre,
    @NotNull TipoFormaBarra tipoForma,
    @NotNull @Min(2) @Max(50) Integer cantidadSillas,
    @PositiveOrZero Integer orden
) {}