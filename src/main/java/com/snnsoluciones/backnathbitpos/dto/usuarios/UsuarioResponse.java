package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor  // Agrega constructor sin argumentos
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private String telefono;
    private RolNombre rol;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public List<String> empresas;     // Lista de nombres de empresas asignadas
    public List<String> sucursales;   // Lista de nombres de sucursales asignadas
}