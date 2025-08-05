package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO para asignar sucursales
@Data
class AsignarSucursalesRequest {
  @NotNull
  private Set<UUID> sucursalesIds;

  private Map<UUID, PermisosSucursal> permisosPorSucursal;

  @Data
  static class PermisosSucursal {
    private Boolean puedeLeer = true;
    private Boolean puedeEscribir = true;
    private Boolean puedeEliminar = false;
    private Boolean puedeAprobar = false;
  }
}