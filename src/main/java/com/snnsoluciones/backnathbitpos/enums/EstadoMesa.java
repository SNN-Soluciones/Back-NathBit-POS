// src/main/java/.../enums/mesa/EstadoMesa.java
package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoMesa {
  LIBRE,          // disponible
  OCUPADA,        // en servicio
  RESERVADA,      // reserva futura/activa
  BLOQUEADA,      // fuera de servicio
  LIMPIEZA,       // esperando limpieza
  CUENTA_SOLICITADA, // cliente pidió cuenta
  CUENTA_ENTREGADA   // cuenta impresa/entregada
}