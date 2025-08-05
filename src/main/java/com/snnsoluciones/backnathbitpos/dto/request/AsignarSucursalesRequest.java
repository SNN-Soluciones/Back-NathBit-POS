package com.snnsoluciones.backnathbitpos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO para asignar sucursales a un usuario con permisos específicos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para asignar sucursales a un usuario")
public class AsignarSucursalesRequest {

    @NotNull(message = "Debe especificar al menos una sucursal")
    @Schema(description = "IDs de las sucursales a asignar")
    private Set<UUID> sucursalesIds;

    @Schema(description = "ID de la sucursal principal (opcional)")
    private UUID sucursalPrincipalId;

    @Schema(description = "Permisos específicos por sucursal (opcional). Si no se especifica, se usan permisos por defecto")
    private Map<UUID, PermisosSucursal> permisosPorSucursal;

    @Schema(description = "Aplicar mismos permisos a todas las sucursales", defaultValue = "false")
    @Builder.Default
    private Boolean aplicarPermisosPorDefecto = false;

    @Schema(description = "Permisos por defecto si aplicarPermisosPorDefecto es true")
    private PermisosSucursal permisosPorDefecto;

    /**
     * DTO para permisos específicos de sucursal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Permisos específicos para una sucursal")
    public static class PermisosSucursal {

        @Schema(description = "Permiso de lectura", defaultValue = "true")
        @Builder.Default
        private Boolean puedeLeer = true;

        @Schema(description = "Permiso de escritura", defaultValue = "true")
        @Builder.Default
        private Boolean puedeEscribir = true;

        @Schema(description = "Permiso de eliminación", defaultValue = "false")
        @Builder.Default
        private Boolean puedeEliminar = false;

        @Schema(description = "Permiso de aprobación", defaultValue = "false")
        @Builder.Default
        private Boolean puedeAprobar = false;

        /**
         * Crea permisos básicos (lectura y escritura)
         */
        public static PermisosSucursal basicos() {
            return PermisosSucursal.builder()
                .puedeLeer(true)
                .puedeEscribir(true)
                .puedeEliminar(false)
                .puedeAprobar(false)
                .build();
        }

        /**
         * Crea permisos completos
         */
        public static PermisosSucursal completos() {
            return PermisosSucursal.builder()
                .puedeLeer(true)
                .puedeEscribir(true)
                .puedeEliminar(true)
                .puedeAprobar(true)
                .build();
        }

        /**
         * Crea permisos de solo lectura
         */
        public static PermisosSucursal soloLectura() {
            return PermisosSucursal.builder()
                .puedeLeer(true)
                .puedeEscribir(false)
                .puedeEliminar(false)
                .puedeAprobar(false)
                .build();
        }
    }

    /**
     * Obtiene los permisos para una sucursal específica
     */
    public PermisosSucursal getPermisosParaSucursal(UUID sucursalId) {
        // Si hay permisos específicos para esta sucursal
        if (permisosPorSucursal != null && permisosPorSucursal.containsKey(sucursalId)) {
            return permisosPorSucursal.get(sucursalId);
        }

        // Si se deben aplicar permisos por defecto
        if (Boolean.TRUE.equals(aplicarPermisosPorDefecto) && permisosPorDefecto != null) {
            return permisosPorDefecto;
        }

        // Permisos básicos por defecto
        return PermisosSucursal.basicos();
    }
}