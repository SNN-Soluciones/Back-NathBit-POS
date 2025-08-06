package com.snnsoluciones.backnathbitpos.enums;

public enum TipoAcceso {
    OPERATIVO,      // Usuario con un solo rol operativo (cajero, mesero, cocina)
    ADMINISTRATIVO, // Usuario con rol admin/jefe en una empresa
    MULTIPLE       // Usuario con múltiples accesos
}