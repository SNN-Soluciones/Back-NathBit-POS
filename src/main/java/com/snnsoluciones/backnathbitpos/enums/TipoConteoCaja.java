package com.snnsoluciones.backnathbitpos.enums;

public enum TipoConteoCaja {
    APERTURA,       // Ana al abrir la caja
    TURNO,          // Pedro o Juan al cerrar su turno
    CIERRE_FINAL    // El último — dispara cierre de SesionCaja
}