package com.snnsoluciones.backnathbitpos.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
   * DTO para información de sucursal
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class SucursalInfo {
    private UUID sucursalId;
    private String codigoSucursal;
    private String nombreSucursal;
    private String schemaName; // tenant_id
    private boolean esPrincipal;
    private boolean activa;
  }