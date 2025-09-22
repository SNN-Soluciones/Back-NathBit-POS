package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.dto.auth.EmpresaResumen;
import com.snnsoluciones.backnathbitpos.dto.auth.SucursalResumen;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import java.util.ArrayList;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UsuarioListadoResponse {
    private Long id;
    private String email;
    private String nombre;
    private String apellidos;
    private String telefono;
    private RolNombre rol;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Asignaciones
    private List<EmpresaResumen> empresas = new ArrayList<>();
    private List<SucursalResumen> sucursales = new ArrayList<>();
    private Boolean tieneAsignacion;
}