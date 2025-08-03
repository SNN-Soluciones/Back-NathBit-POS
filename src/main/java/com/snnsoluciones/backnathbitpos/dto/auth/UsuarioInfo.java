package com.snnsoluciones.backnathbitpos.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
   * DTO con información básica del usuario
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class UsuarioInfo {
    private UUID id;
    private String email;
    private String nombre;
    private String apellidos;
    private String nombreCompleto;
    private boolean requiereCambioPassword;
  }