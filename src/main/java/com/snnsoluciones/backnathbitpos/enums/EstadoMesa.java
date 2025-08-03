// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/enums/EstadoMesa.java

package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoMesa {
  LIBRE,          // Mesa disponible
  OCUPADA,        // Mesa con clientes
  RESERVADA,      // Mesa reservada
  BLOQUEADA,      // Mesa no disponible (mantenimiento, etc.)
  EN_LIMPIEZA,    // Mesa siendo limpiada
  CUENTA_PEDIDA   // Clientes pidieron la cuenta
}