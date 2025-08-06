package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UsuarioEmpresaRolDTO {
    private Long id;
    private Long usuarioId;
    private Long empresaId;
    private String empresaNombre;
    private Long sucursalId;
    private String sucursalNombre;
    private RolNombre rol;
    private Map<String, Map<String, Boolean>> permisos;
    private Boolean esPrincipal;
    private Boolean activo;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaVencimiento;
    private String descripcionCompleta;
}