package com.snnsoluciones.backnathbitpos.dto.mesas;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ActualizarMesaRequest(
  @NotBlank @Size(max=20) String codigo,
  @Size(max=60) String nombre,
  @Min(1) Integer capacidad,
  @PositiveOrZero Integer orden,
  Boolean activa
) {}
