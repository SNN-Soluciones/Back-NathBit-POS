package com.snnsoluciones.backnathbitpos.dto.auth;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
   * DTO para representar un contexto de acceso (empresa o sucursal)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class ContextoAcceso {
    private UUID empresaId;
    private String empresaCodigo;
    private String empresaNombre;
    private String empresaLogo;
    private String rol;
    private boolean esPropietario;
    private List<SucursalInfo> sucursalesDisponibles;
    private int cantidadSucursales;
  }