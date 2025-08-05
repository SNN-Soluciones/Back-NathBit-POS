package com.snnsoluciones.backnathbitpos.enums;

import lombok.Getter;

/**
 * Enum que define los roles disponibles en el sistema
 */
@Getter
public enum RolNombre {

    ROOT(1, "Root", "Desarrollador con acceso total al sistema"),
    SUPER_ADMIN(2, "Super Administrador", "Administrador de múltiples empresas"),
    ADMIN(3, "Administrador", "Administrador de empresa"),
    JEFE_CAJAS(4, "Jefe de Cajas", "Supervisor de cajas y cierres"),
    CAJERO(5, "Cajero", "Operador de punto de venta"),
    MESERO(6, "Mesero", "Toma pedidos y gestiona mesas"),
    COCINA(7, "Cocina", "Vista de órdenes para preparación");

    private final int nivel;
    private final String descripcion;
    private final String detalle;

    RolNombre(int nivel, String descripcion, String detalle) {
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.detalle = detalle;
    }

    /**
     * Verifica si este rol tiene mayor jerarquía que otro
     */
    public boolean esSuperiorA(RolNombre otroRol) {
        return this.nivel < otroRol.nivel;
    }

    /**
     * Verifica si este rol tiene mayor o igual jerarquía que otro
     */
    public boolean esSuperiorOIgualA(RolNombre otroRol) {
        return this.nivel <= otroRol.nivel;
    }

    /**
     * Verifica si es un rol administrativo (puede gestionar usuarios)
     */
    public boolean esAdministrativo() {
        return this == ROOT || this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Verifica si es un rol operativo (trabajador de sucursal)
     */
    public boolean esOperativo() {
        return this == CAJERO || this == MESERO || this == COCINA;
    }

    /**
     * Verifica si es un rol de supervisión
     */
    public boolean esSupervisor() {
        return this == JEFE_CAJAS;
    }

    /**
     * Obtiene el rol por su nombre
     */
    public static RolNombre fromString(String nombre) {
        for (RolNombre rol : RolNombre.values()) {
            if (rol.name().equalsIgnoreCase(nombre)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + nombre);
    }
}