package com.snnsoluciones.backnathbitpos.dto.auth.multitenant;

import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs para el sistema de autenticación multi-tenant
 */
public class AuthMultitenantDTOs {

    // ==================== LOGIN GLOBAL ====================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarDispositivoConCredencialesRequest {
        @NotNull(message = "El tenantId es requerido")
        private Long tenantId;

        @NotNull(message = "La sucursal es requerida")
        private Long sucursalId;

        @NotBlank(message = "El nombre del dispositivo es requerido")
        @Size(max = 100)
        private String nombreDispositivo;

        private String plataforma; // WEB, ANDROID, IOS

        private String tipo;       // "PDV" | "KIOSKO" | "COCINA"
        private Long terminalId;
    }

    /**
     * Request para login de usuario global (ROOT, SOPORTE, SUPER_ADMIN)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginGlobalRequest {
        @NotBlank(message = "El email es requerido")
        @Email(message = "Email inválido")
        private String email;

        @NotBlank(message = "La contraseña es requerida")
        private String password;
    }

    /**
     * Response del login global
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginGlobalResponse {
        private String token;
        private String refreshToken;
        private UsuarioGlobalInfo usuario;
        private List<TenantResumen> tenants;
        private boolean requiereSeleccionTenant;
    }

    /**
     * Info básica del usuario global
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioGlobalInfo {
        private Long id;
        private String email;
        private String nombre;
        private String apellidos;
        private String nombreCompleto;
        private String rol;
        private boolean requiereCambioPassword;
    }

    /**
     * Resumen de tenant para listados
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantResumen {
        private Long id;
        private String codigo;
        private String nombre;
        private boolean esPropietario;
        private List<SucursalResumen> sucursales;
    }

    // ==================== LOGIN POR EMPRESA ====================

    /**
     * Request para login por empresa (desde dispositivo)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginEmpresaRequest {
        @NotBlank(message = "El código de empresa es requerido")
        private String codigo;
    }

    /**
     * Response del login por empresa
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginEmpresaResponse {
        private TenantResumen tenant;
        private DispositivoInfo dispositivo;
        private List<UsuarioLocalInfo> usuarios;
        private boolean requiereRegistro;
        private List<SucursalResumen> sucursales; // ← NUEVO (para Flujo A)
    }

    /**
     * Info del dispositivo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispositivoInfo {
        private Long id;
        private String nombre;
        private String plataforma;
        private LocalDateTime ultimoUso;
        private SucursalResumen sucursal;  // NUEVO
    }

    /**
     * Info de usuario local (para selección)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioLocalInfo {
        private Long id;
        private String nombre;
        private String apellidos;
        private String nombreCompleto;
        private String rol;
        private String avatar; // URL o iniciales
        private String fuente; // "GLOBAL" | "SCHEMA"
    }

    // ==================== REGISTRO DE DISPOSITIVO ====================

    /**
     * Request para solicitar código OTP
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SolicitarCodigoRequest {
        @NotBlank(message = "El código del tenant es requerido")
        private String tenantCodigo;

        @NotBlank(message = "El nombre del dispositivo es requerido")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        private String nombreDispositivo;

        @NotNull(message = "La sucursal es requerida")
        private Long sucursalId;

        private String plataforma; // WEB, ANDROID, IOS, WINDOWS
    }

    /**
     * Response de solicitud de código
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SolicitarCodigoResponse {
        private String mensaje;
        private int expiraEnSegundos;
        private int intentosRestantes;
    }

    /**
     * Request para verificar código OTP
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificarCodigoRequest {
        @NotBlank(message = "El código del tenant es requerido")
        private String tenantCodigo;

        @NotBlank(message = "El nombre del dispositivo es requerido")
        private String nombreDispositivo;

        @NotBlank(message = "El código OTP es requerido")
        @Size(min = 6, max = 6, message = "El código debe tener 6 dígitos")
        private String codigo;

        @NotNull(message = "La sucursal es requerida")
        private Long sucursalId;


        private String tipo;       // "PDV" | "KIOSKO" | "COCINA"
        private Long terminalId;
        private String plataforma;

    }

    /**
     * Response de verificación exitosa
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificarCodigoResponse {
        private String deviceToken;
        private TenantResumen tenant;
        private DispositivoInfo dispositivo;
        private SucursalResumen sucursal;  // NUEVO
    }

    // ==================== AUTH PIN ====================

    /**
     * Request para login con PIN
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginPinRequest {
        @NotNull(message = "El ID del usuario es requerido")
        private Long usuarioId;

        @NotBlank(message = "El PIN es requerido")
        @Size(min = 4, max = 6, message = "El PIN debe tener entre 4 y 6 dígitos")
        private String pin;

        private String fuente; // "GLOBAL" | "SCHEMA"

    }

    /**
     * Response del login con PIN
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginPinResponse {
        private String sessionToken;
        private UsuarioLocalInfo usuario;
        private TenantResumen tenant;
        private List<SucursalResumen> sucursales;
        private boolean requiereCambioPin;
    }

    /**
     * Resumen de sucursal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SucursalResumen {
        private Long id;
        private String nombre;
        private String numeroSucursal;
    }

    /**
     * Request para obtener usuarios del dispositivo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObtenerUsuariosResponse {
        private TenantResumen tenant;
        private SucursalResumen sucursal;  // NUEVO
        private DispositivoInfo dispositivo;
        private List<UsuarioLocalInfo> usuarios;
    }

    /**
     * Response con lista de sucursales del tenant
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListaSucursalesResponse {
        private TenantResumen tenant;
        private List<SucursalResumen> sucursales;
    }

    // ==================== CERRAR SESIÓN ====================

    /**
     * Response genérico de éxito
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MensajeResponse {
        private String mensaje;
        private boolean success;
    }
}
