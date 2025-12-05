package com.snnsoluciones.backnathbitpos.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para administración de Tenants y Dispositivos
 */
public class TenantAdminDTOs {

    // ==================== TENANT DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTenantRequest {
        @NotBlank(message = "La razón social es requerida")
        @Size(max = 200, message = "La razón social no puede exceder 200 caracteres")
        private String razonSocial;

        @Size(max = 50, message = "El código no puede exceder 50 caracteres")
        private String codigo;

        private Long empresaLegacyId;

        @Email(message = "Email inválido")
        private String superAdminEmail;

        private String superAdminNombre;

        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        private String superAdminPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTenantRequest {
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        private String nombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantDetailResponse {
        private Long id;
        private String codigo;
        private String nombre;
        private String schemaName;
        private Long empresaLegacyId;
        private Boolean activo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long dispositivosActivos;
        private Long dispositivosTotales;
        private Long superAdminsAsignados;
        private List<SuperAdminInfo> superAdmins;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantListResponse {
        private Long id;
        private String codigo;
        private String nombre;
        private String schemaName;
        private Boolean activo;
        private Long dispositivosActivos;
        private Long superAdminsAsignados;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuperAdminInfo {
        private Long id;
        private String email;
        private String nombre;
        private String apellidos;
        private Boolean esPropietario;
        private Boolean activo;
    }

    // ==================== DISPOSITIVO DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispositivoDetailResponse {
        private Long id;
        private String nombre;
        private String token;
        private String plataforma;
        private String userAgent;
        private String ipRegistro;
        private Boolean activo;
        private LocalDateTime ultimoUso;
        private LocalDateTime createdAt;
        private Long tenantId;
        private String tenantCodigo;
        private String tenantNombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispositivoListResponse {
        private Long id;
        private String nombre;
        private String plataforma;
        private String ipRegistro;
        private Boolean activo;
        private LocalDateTime ultimoUso;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsignarSuperAdminRequest {
        @NotNull(message = "El ID del usuario es requerido")
        private Long usuarioId;

        @Builder.Default
        private Boolean esPropietario = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasGlobales {
        private Long totalTenants;
        private Long tenantsActivos;
        private Long totalDispositivos;
        private Long dispositivosActivos;
        private Long totalUsuariosGlobales;
        private Long totalSuperAdmins;
    }
}