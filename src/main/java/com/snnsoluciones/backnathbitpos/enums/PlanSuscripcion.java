package com.snnsoluciones.backnathbitpos.enums;


import lombok.Getter;

/**
 * Planes de suscripción disponibles
 */
@Getter
public enum PlanSuscripcion {

  BASICO(1, "Plan Regimen Simplificado", 100, 10, 3),
  PROFESIONAL(5, "Plan Profesional", 200, 5, 20),
  EMPRESARIAL(0, "Plan Empresarial", 0, 0, 0), // Sin límites
  PERSONALIZADO(0, "Plan Personalizado", 0, 0, 0); // Configuración especial

  private final int maxSucursales;
  private final String descripcion;
  private final int maxDocuentosElectronicos;
  private final int maxMesas;
  private final int maxUsuarios;

  PlanSuscripcion(int maxSucursales, String descripcion,
      int maxDocuentosElectronicos, int maxMesas, int maxUsuarios) {
    this.maxSucursales = maxSucursales;
    this.descripcion = descripcion;
    this.maxDocuentosElectronicos = maxDocuentosElectronicos;
    this.maxMesas = maxMesas;
    this.maxUsuarios = maxUsuarios;
  }

  /**
   * Verifica si el plan tiene límite de sucursales
   */
  public boolean tieneLimiteSucursales() {
    return maxSucursales > 0;
  }

  /**
   * Verifica si se puede agregar otra sucursal
   */
  public boolean puedeAgregarSucursal(int sucursalesActuales) {
    return !tieneLimiteSucursales() || sucursalesActuales < maxSucursales;
  }

  /**
   * Verifica si el plan tiene algún límite
   */
  public boolean tieneLimites() {
    return maxSucursales > 0 || maxDocuentosElectronicos > 0 ||
        maxMesas > 0 || maxUsuarios > 0;
  }
}