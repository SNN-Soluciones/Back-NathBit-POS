package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoOrden {
  ABIERTA("Abierta", "Orden activa, puede modificarse"),
  EN_PREPARACION("En Preparación", "Orden enviada a cocina"),
  PREPARADA("Preparada", "Orden lista para servir"),
  SERVIDA("Servida", "Orden entregada al cliente"),
  POR_PAGAR("Por Pagar", "Cliente solicitó la cuenta"),
  PAGADA("Pagada", "Orden pagada y cerrada"),
  ANULADA("Anulada", "Orden cancelada"),
  SPLIT("Split", "Orden dividida en varias cuentas");

  private final String descripcion;
  private final String detalle;

  EstadoOrden(String descripcion, String detalle) {
    this.descripcion = descripcion;
    this.detalle = detalle;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getDetalle() {
    return detalle;
  }

  public boolean puedeModificarse() {
    return this == ABIERTA;
  }

  public boolean puedeEnviarCocina() {
    return this == ABIERTA || this == EN_PREPARACION;
  }

  public boolean puedePagarse() {
    return this == ABIERTA || this == EN_PREPARACION || this == PREPARADA ||
        this == SERVIDA || this == POR_PAGAR;
  }

  public boolean esFinal() {
    return this == PAGADA || this == ANULADA;
  }
}