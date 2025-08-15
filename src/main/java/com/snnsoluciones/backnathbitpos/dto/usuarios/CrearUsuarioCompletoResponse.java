package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearUsuarioCompletoResponse {

    private Long usuarioId;
    private String email;
    private String nombre;
    private String apellidos;
    private RolNombre rol;
    
    // Contraseña temporal si se generó
    private String passwordTemporal;
    
    // Resumen de asignaciones
    private List<String> empresasAsignadas;
    private List<String> sucursalesAsignadas;
    
    // Metadata
    private String mensaje;
    private LocalDateTime createdAt;
    private String creadoPor;
}