package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private RolNombre rol;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}