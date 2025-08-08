package com.snnsoluciones.backnathbitpos.enums;

/**
 * Enum que define los tipos de usuario en el sistema.
 * SISTEMA: Usuarios ROOT y DEVELOPER que no requieren empresa
 * EMPRESARIAL: Todos los demás usuarios que requieren asignación a empresa/sucursal
 */
public enum TipoUsuario {
    SISTEMA("Sistema", "Usuarios con acceso total al sistema"),
    EMPRESARIAL("Empresarial", "Dueños de empresas, único usuario requerido"),
    GERENCIAL("Administradores de sucursales", "Administradores asignados a sucursales"),
    OPERATIVO("Operativo", "Usuarios operativos por sucursal");

    private final String displayName;
    private final String descripcion;

    TipoUsuario(String displayName, String descripcion) {
        this.displayName = displayName;
        this.descripcion = descripcion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Verifica si el usuario es de tipo sistema (ROOT o DEVELOPER)
     */
    public boolean esSistema() {
        return this == SISTEMA;
    }

    private Boolean requiereScopeEmpresa() {
        return this == EMPRESARIAL;
    }

    /**
     * Verifica si el usuario requiere asignación a empresa
     */
    public boolean requiereEmpresa() {
        return this == GERENCIAL;
    }

    /**
     * Verifica si el usuario requere a sucursal
     */
    public boolean requiereSucursal() {
        return this == OPERATIVO;
    }
}