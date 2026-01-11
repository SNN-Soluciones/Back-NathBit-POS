package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

/**
 * DTO simple para usuarios en el dashboard
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSimpleDTO {
    
    /**
     * ID del usuario
     */
    private Long id;
    
    /**
     * Nombre completo del usuario
     */
    private String nombre;
    
    /**
     * Username/login del usuario
     */
    private String usuario;
    
    /**
     * Rol del usuario
     */
    private String rol;
    
    /**
     * Si el usuario está activo
     */
    private Boolean activo;
}