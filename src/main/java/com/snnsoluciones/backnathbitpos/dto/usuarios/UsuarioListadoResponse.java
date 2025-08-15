package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UsuarioListadoResponse {
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private RolNombre rol;
    private Boolean activo;
    
    // Asignaciones
    private List<String> empresas;
    private List<String> sucursales;
    
    // Auditoría
    private String creadoPor;
    private LocalDateTime fechaCreacion;
}