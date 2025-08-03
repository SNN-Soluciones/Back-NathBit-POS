// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/enums/EstadoOrden.java

package com.snnsoluciones.backnathbitpos.enums;

public enum EstadoOrden {
  PENDIENTE,      // Orden creada
  EN_COCINA,      // En preparación
  LISTA,          // Lista para servir/entregar
  SERVIDA,        // Servida al cliente
  PAGADA,         // Pagada
  CANCELADA,      // Cancelada
  DEVUELTA        // Devuelta/Reembolsada
}