package com.snnsoluciones.backnathbitpos.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
   * DTO para acceso directo a sucursal (cajeros/meseros)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class SucursalDirecta {
    private UUID empresaId;
    private String empresaNombre;
    private UUID sucursalId;
    private String sucursalNombre;
    private String schemaName; // tenant_id
    private String rol;
    private String urlRedirect; // URL específica para redirigir
  }