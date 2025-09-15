// dto/zonas
package com.snnsoluciones.backnathbitpos.dto.mesas;

import jakarta.validation.constraints.*;

public record CrearZonaRequest(
  @NotBlank @Size(max=60) String nombre,
  @Size(max=200) String descripcion,
  @PositiveOrZero Integer orden
) {}
