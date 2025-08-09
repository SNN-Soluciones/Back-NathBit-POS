package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class AsignarRolRequest {

    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    private Long sucursalId; // Opcional - null = todas las sucursales

    private Long usuarioId;

    @NotNull(message = "El rol es requerido")
    private RolNombre rol;

    private Map<String, Map<String, Boolean>> permisos;

    private Boolean esPrincipal = false;

    private String notas;
}