package com.snnsoluciones.backnathbitpos.dto.usuario;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsuarioDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private String nombreCompleto;
    private String telefono;
    private String identificacion;
    private Boolean activo;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime createdAt;
}