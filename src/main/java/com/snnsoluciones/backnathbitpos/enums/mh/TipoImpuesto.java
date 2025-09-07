package com.snnsoluciones.backnathbitpos.enums.mh;

import java.util.Arrays;
import java.util.Optional;

public enum TipoImpuesto {
  IVA("01", "Impuesto al Valor Agregado"),
  SELECTIVO_CONSUMO("02", "Impuesto Selectivo de Consumo"),
  COMBUSTIBLES("03", "Impuesto Único a los Combustibles"),
  SERVICIO("10", "Impuesto sobre el servicio (10%)"),
  OTROS("99", "Otros");

  private final String codigo;
  private final String descripcion;

  TipoImpuesto(String codigo, String descripcion) {
    this.codigo = codigo;
    this.descripcion = descripcion;
  }
  public String getCodigo() { return codigo; }
  public String getDescripcion() { return descripcion; }

  Optional<?> fromCodigoOptional(String codigo) {
    if (codigo == null) return Optional.empty();

    return Arrays.stream(values())
        .filter(tipo -> tipo.codigo.equals(codigo))
        .findFirst();
  }

  public static TipoImpuesto fromCodigo(String codigo) {
    if (codigo == null) {
      return null;
    }
    for (TipoImpuesto tipo : values()) {
      if (tipo.codigo.equals(codigo)) {
        return tipo;
      }
    }
    throw new IllegalArgumentException("Código Impuesto no valido");
  }
}