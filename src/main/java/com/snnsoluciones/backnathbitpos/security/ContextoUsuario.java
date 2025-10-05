package com.snnsoluciones.backnathbitpos.security;

import lombok.Data;

/**
 * Clase que mantiene el contexto del usuario autenticado
 * incluyendo empresa y sucursal seleccionadas
 */
@Data
public class ContextoUsuario {
    private Long userId;
    private String email;
    private String rol;
    private Long empresaId;
    private Long sucursalId;
}