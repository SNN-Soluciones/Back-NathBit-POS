package com.snnsoluciones.backnathbitpos.dto.auth;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.Data;

@Data
public class ContextoDTO {
    private Long usuarioId;
    private Long empresaId;
    private Long sucursalId;
    private RolNombre rol;
}