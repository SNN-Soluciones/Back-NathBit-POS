package com.snnsoluciones.backnathbitpos.dto.mesas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ActualizarZonaRequest(
  @NotBlank @Size(max=60) String nombre,
  @Size(max=200) String descripcion,
  @PositiveOrZero Integer orden,
  Boolean activo
) {}
