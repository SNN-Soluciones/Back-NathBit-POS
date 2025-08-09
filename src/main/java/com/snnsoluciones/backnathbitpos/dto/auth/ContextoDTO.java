package com.snnsoluciones.backnathbitpos.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;

/**
 * DTO para el contexto de trabajo actual del usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextoDTO {
    
    // Usuario
    private Long usuarioId;
    
    // Empresa
    private Long empresaId;
    private String empresaNombre;
    private String empresaCodigo;
    
    // Sucursal (null = todas las sucursales)
    private Long sucursalId;
    private String sucursalNombre;
    private String sucursalCodigo;
    
    // Permisos específicos en este contexto
    private Map<String, Object> permisos;
    
    // Metadata
    private Long establecidoEn;
    private Long ultimaActividad;
    private Boolean esPorDefecto;
    
    /**
     * Verifica si el contexto tiene acceso a todas las sucursales
     */
    public boolean tieneAccesoTodasSucursales() {
        return sucursalId == null;
    }
    
    /**
     * Verifica si tiene un permiso específico
     */
    public boolean tienePermiso(String modulo, String accion) {
        if (permisos == null || permisos.isEmpty()) {
            return false;
        }
        
        // Si tiene acceso_total, puede hacer todo
        if (Boolean.TRUE.equals(permisos.get("acceso_total"))) {
            return true;
        }
        
        // Verificar permiso específico
        if (permisos.containsKey(modulo)) {
            Object moduloPermisos = permisos.get(modulo);
            if (moduloPermisos instanceof Map) {
                Map<String, Object> permisoMap = (Map<String, Object>) moduloPermisos;
                return Boolean.TRUE.equals(permisoMap.get(accion));
            }
        }
        
        return false;
    }
    
    /**
     * Obtiene el tiempo en milisegundos desde que se estableció el contexto
     */
    public long getTiempoDesdeEstablecido() {
        if (establecidoEn == null) return 0;
        return System.currentTimeMillis() - establecidoEn;
    }
    
    /**
     * Verifica si el contexto está activo (menos de 8 horas)
     */
    public boolean estaActivo() {
        return getTiempoDesdeEstablecido() < (8 * 60 * 60 * 1000); // 8 horas
    }
}