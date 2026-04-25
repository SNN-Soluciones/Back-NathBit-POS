// Path: src/main/java/com/snnsoluciones/backnathbitpos/controller/auth/PdvRegistroController.java

package com.snnsoluciones.backnathbitpos.controller.auth.pdv;

import com.snnsoluciones.backnathbitpos.dto.auth.pdv.PdvRegistroDTOs.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.auth.pdv.PdvRegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/pdv")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDV Registro", description = "Registro de dispositivos PDV — Flujo A (OTP) y Flujo B (credenciales)")
public class PdvRegistroController {

    private final PdvRegistroService pdvRegistroService;

    // ── 1. SOLICITAR CÓDIGO ────────────────────────────────────────────────
    @Operation(summary = "Solicitar OTP para registrar dispositivo",
               description = "Envía código de 6 dígitos al correo de SNN y a los SUPER_ADMIN del tenant. Público.")
    @PostMapping("/solicitar-codigo")
    public ResponseEntity<ApiResponse<SolicitarCodigoResponse>> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/pdv/solicitar-codigo - tenant: {}", request.getTenantCodigo());
        SolicitarCodigoResponse response = pdvRegistroService.solicitarCodigo(
            request,
            obtenerIp(httpRequest),
            httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.ok(response.getMensaje(), response));
    }

    // ── 2. VALIDAR CÓDIGO ──────────────────────────────────────────────────
    @Operation(summary = "Validar OTP y obtener sucursales",
               description = "Valida el código OTP. Retorna registrationToken (5 min) y lista de sucursales. Público.")
    @PostMapping("/validar-codigo")
    public ResponseEntity<ApiResponse<ValidarCodigoResponse>> validarCodigo(
            @Valid @RequestBody ValidarCodigoRequest request) {

        log.info("POST /api/auth/pdv/validar-codigo - tenant: {}", request.getTenantCodigo());
        ValidarCodigoResponse response = pdvRegistroService.validarCodigo(request);
        return ResponseEntity.ok(ApiResponse.ok("Código válido", response));
    }

    // ── 3. REGISTRAR DISPOSITIVO ───────────────────────────────────────────
    @Operation(summary = "Registrar dispositivo PDV",
               description = """
                   Flujo A: enviar registrationToken en el body (obtenido de /validar-codigo).
                   Flujo B: enviar JWT global en Authorization header y tenantId en el body.
                   Público pero requiere registrationToken O JWT válido.
                   """)
    @PostMapping("/registrar-dispositivo")
    public ResponseEntity<ApiResponse<RegistrarDispositivoResponse>> registrarDispositivo(
            @Valid @RequestBody RegistrarDispositivoRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/pdv/registrar-dispositivo - dispositivo: {}", request.getNombreDispositivo());

        // Extraer usuarioId si hay JWT (Flujo B). Si no hay auth es Flujo A.
        Long usuarioGlobalId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                usuarioGlobalId = ((ContextoUsuario) authentication.getPrincipal()).getUserId();
            } catch (Exception e) {
                log.debug("Sin usuario autenticado — usando registrationToken (Flujo A)");
            }
        }

        RegistrarDispositivoResponse response = pdvRegistroService.registrarDispositivo(
            request,
            usuarioGlobalId,
            obtenerIp(httpRequest),
            httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Dispositivo registrado exitosamente", response));
    }

    // ── 4. LOGIN CON CREDENCIALES ──────────────────────────────────────────
    @Operation(summary = "Login con email+password para registro de dispositivo",
               description = "Solo ROOT, SOPORTE y SUPER_ADMIN. Retorna JWT + tenants con sus sucursales. Público.")
    @PostMapping("/login-credenciales")
    public ResponseEntity<ApiResponse<LoginCredencialesResponse>> loginCredenciales(
            @Valid @RequestBody LoginCredencialesRequest request) {

        log.info("POST /api/auth/pdv/login-credenciales - email: {}", request.getEmail());
        LoginCredencialesResponse response = pdvRegistroService.loginCredenciales(request);
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri;
        return request.getRemoteAddr();
    }
}