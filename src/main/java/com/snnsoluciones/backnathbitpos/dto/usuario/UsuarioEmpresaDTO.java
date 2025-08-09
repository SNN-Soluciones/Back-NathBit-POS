package com.snnsoluciones.backnathbitpos.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEmpresaDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long empresaId;
    private String empresaNombre;
    private String empresaCodigo;
    private Long sucursalId;
    private String sucursalNombre;
    private String sucursalCodigo;
    private Boolean accesoTodasSucursales;
    private Boolean activo;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaRevocacion;
    private String notas;
}