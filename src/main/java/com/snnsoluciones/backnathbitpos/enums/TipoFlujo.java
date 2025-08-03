package com.snnsoluciones.backnathbitpos.enums;

/**
   * Enum que define el tipo de flujo post-login
   */
  public enum TipoFlujo {
    DIRECTO_POS,        // Cajero/Mesero - va directo al POS
    SELECTOR_EMPRESA,   // Admin - debe seleccionar empresa
    SELECTOR_SUCURSAL,  // Usuario con múltiples sucursales en una empresa
    SIN_ACCESO         // Usuario sin accesos activos
  }