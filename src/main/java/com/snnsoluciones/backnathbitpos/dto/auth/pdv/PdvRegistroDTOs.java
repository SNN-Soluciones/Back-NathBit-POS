// Path: src/main/java/com/snnsoluciones/backnathbitpos/dto/auth/pdv/PdvRegistroDTOs.java

package com.snnsoluciones.backnathbitpos.dto.auth.pdv;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

public class PdvRegistroDTOs {

    // ── Endpoint 1: solicitar-codigo ───────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitarCodigoRequest {
        @NotBlank(message = "El código de empresa es requerido")
        private String tenantCodigo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SolicitarCodigoResponse {
        private String tenantNombre;
        private String mensaje;
        private int expiraEnSegundos;
    }

    // ── Endpoint 2: validar-codigo ─────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidarCodigoRequest {
        @NotBlank(message = "El código de empresa es requerido")
        private String tenantCodigo;

        @NotBlank(message = "El código OTP es requerido")
        @Size(min = 6, max = 6, message = "El código debe tener 6 dígitos")
        private String codigo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ValidarCodigoResponse {
        private String registrationToken; // JWT de 5 min — usar en Endpoint 3
        private Long tenantId;
        private String tenantNombre;
        private List<SucursalSimple> sucursales;
    }

    // ── Endpoint 3: registrar-dispositivo ─────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarDispositivoRequest {
        // Flujo A: viene el registrationToken
        private String registrationToken;

        // Flujo B: viene el tenantId del JWT global (se extrae del token auth)
        // pero si se quiere pasar explícito también se acepta
        private Long tenantId;

        @NotNull(message = "La sucursal es requerida")
        private Long sucursalId;

        @NotBlank(message = "El nombre del dispositivo es requerido")
        @Size(max = 100)
        private String nombreDispositivo;

        private String tipo;

        private Long terminalId;

        private String plataforma; // WEB | ANDROID | IOS
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegistrarDispositivoResponse {
        private String deviceToken;
        private Long tenantId;
        private String tenantNombre;
        private Long sucursalId;
        private String sucursalNombre;
        private Long dispositivoId;
        private String dispositivoNombre;
        private String plataforma;
    }

    // ── Endpoint 4: login-credenciales ────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginCredencialesRequest {
        @NotBlank(message = "El email es requerido")
        @Email
        private String email;

        @NotBlank(message = "La contraseña es requerida")
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginCredencialesResponse {
        private String token;         // JWT para usar en Endpoint 3 Flujo B
        private UsuarioInfo usuario;
        private List<TenantConSucursales> tenants;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UsuarioInfo {
        private Long id;
        private String nombre;
        private String apellidos;
        private String email;
        private String rol;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TenantConSucursales {
        private Long id;
        private String codigo;
        private String nombre;
        private List<SucursalSimple> sucursales;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SucursalSimple {
        private Long id;
        private String nombre;
        private String numeroSucursal;
    }
}