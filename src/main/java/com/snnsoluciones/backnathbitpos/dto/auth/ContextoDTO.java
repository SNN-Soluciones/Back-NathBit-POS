package com.snnsoluciones.backnathbitpos.dto.auth;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextoDTO {
    private Long usuarioId;
    private Long empresaId;
    private String empresaNombre;
    private String empresaCodigo;
    private Long sucursalId;
    private String sucursalNombre;
    private String sucursalCodigo;
    private RolNombre rol;
    private Map<String, Map<String, Boolean>> permisos;
    private Long establecidoEn; // timestamp cuando se estableció el contexto

    // Métodos helper
    public boolean tienePermiso(String modulo, String accion) {
        if (permisos == null || permisos.isEmpty()) {
            return false;
        }

        Map<String, Boolean> moduloPermisos = permisos.get(modulo);
        return moduloPermisos != null && Boolean.TRUE.equals(moduloPermisos.get(accion));
    }

    public boolean tieneSucursal() {
        return sucursalId != null;
    }

    public String getContextoCompleto() {
        StringBuilder sb = new StringBuilder();
        sb.append(empresaNombre);
        if (sucursalId != null) {
            sb.append(" - ").append(sucursalNombre);
        }
        return sb.toString();
    }
}