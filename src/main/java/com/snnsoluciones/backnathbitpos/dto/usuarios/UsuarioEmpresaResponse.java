package com.snnsoluciones.backnathbitpos.dto.usuarios;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsuarioEmpresaResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long empresaId;
    private String empresaNombre;
    private Long sucursalId;
    private String sucursalNombre;
    private LocalDateTime fechaAsignacion;
    private Boolean activo;
}