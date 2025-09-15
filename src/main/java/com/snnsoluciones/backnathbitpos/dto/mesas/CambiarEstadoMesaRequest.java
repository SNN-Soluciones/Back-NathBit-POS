package com.snnsoluciones.backnathbitpos.dto.mesas;

import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoMesaRequest(
  @NotNull EstadoMesa nuevoEstado,
  @Size(max=160) String motivo,
  Long usuarioId
) {}
