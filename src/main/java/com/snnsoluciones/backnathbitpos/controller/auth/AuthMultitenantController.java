package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.auth.multitenant.AuthDispositivoService;
import com.snnsoluciones.backnathbitpos.service.auth.multitenant.AuthGlobalService;
import com.snnsoluciones.backnathbitpos.service.auth.multitenant.AuthPinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para autenticación multi-tenant.
 * 
 * Endpoints públicos (no requieren autenticación):
 * - POST /api/auth/global/login - Login de usuarios globales
 * - POST /api/auth/empresa - Validar código de empresa
 * - POST /api/auth/dispositivo/solicitar - Solicitar código OTP
 * - POST /api/auth/dispositivo/verificar - Verificar código y registrar dispositivo
 * - POST /api/auth/dispositivo/reenviar - Reenviar código OTP
 * 
 * Endpoints que requieren device token:
 * - GET /api/auth/dispositivo/usuarios - Lista usuarios del tenant
 * - POST /api/auth/pin - Login con PIN
 * 
 * Endpoints que requieren sesión:
 * - POST /api/auth/pin/cambiar - Cambiar PIN
 * - POST /api/auth/cerrar-sesion - Cerrar sesión
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación Multi-Tenant", description = "Endpoints de autenticación para sistema multi-tenant")
public class AuthMultitenantController {

    private final AuthGlobalService authGlobalService;
    private final AuthDispositivoService authDispositivoService;
    private final AuthPinService authPinService;

    /**
     * Header para el token del dispositivo
     */
    private static final String HEADER_DEVICE_TOKEN = "X-Device-Token";

    // ==================== LOGIN GLOBAL ====================

    @Operation(summary = "Login de usuario global", 
               description = "Autenticación con email y password para ROOT, SOPORTE y SUPER_ADMIN")
    @PostMapping("/global/login")
    public ResponseEntity<ApiResponse<LoginGlobalResponse>> loginGlobal(
            @Valid @RequestBody LoginGlobalRequest request) {
        
        log.info("POST /api/auth/global/login - Email: {}", request.getEmail());
        
        try {
            LoginGlobalResponse response = authGlobalService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login exitoso", response));
        } catch (Exception e) {
            log.error("Error en login global: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Seleccionar tenant", 
               description = "Para SUPER_ADMIN con múltiples tenants, genera token con contexto de tenant")
    @PostMapping("/global/seleccionar-tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> seleccionarTenant(
            @PathVariable Long tenantId,
            Authentication authentication) {
        
        log.info("POST /api/auth/global/seleccionar-tenant/{}", tenantId);
        
        try {
            // Obtener ID del usuario desde el token actual
            // Asumiendo que el principal contiene el userId
            Long usuarioId = obtenerUsuarioIdDeAuth(authentication);
            
            String nuevoToken = authGlobalService.generarTokenConTenant(usuarioId, tenantId);
            return ResponseEntity.ok(ApiResponse.success("Tenant seleccionado", nuevoToken));
        } catch (Exception e) {
            log.error("Error seleccionando tenant: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== VALIDACIÓN DE EMPRESA ====================

    @Operation(summary = "Validar código de empresa", 
               description = "Valida el código y verifica si el dispositivo está registrado")
    @PostMapping("/empresa")
    public ResponseEntity<ApiResponse<LoginEmpresaResponse>> validarEmpresa(
            @Valid @RequestBody LoginEmpresaRequest request,
            @RequestHeader(value = HEADER_DEVICE_TOKEN, required = false) String deviceToken,
            HttpServletRequest httpRequest) {
        
        log.info("POST /api/auth/empresa - Código: {}", request.getCodigo());
        
        try {
            String ipCliente = obtenerIpCliente(httpRequest);
            LoginEmpresaResponse response = authDispositivoService.validarEmpresa(
                request.getCodigo(), 
                deviceToken,
                ipCliente
            );
            
            String mensaje = response.isRequiereRegistro() 
                ? "Dispositivo requiere registro" 
                : "Empresa validada";
            
            return ResponseEntity.ok(ApiResponse.success(mensaje, response));
        } catch (Exception e) {
            log.error("Error validando empresa: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== REGISTRO DE DISPOSITIVO ====================

    @Operation(summary = "Solicitar código OTP", 
               description = "Genera y envía código de 6 dígitos a los administradores")
    @PostMapping("/dispositivo/solicitar")
    public ResponseEntity<ApiResponse<SolicitarCodigoResponse>> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("POST /api/auth/dispositivo/solicitar - Tenant: {}, Dispositivo: {}", 
                 request.getTenantCodigo(), request.getNombreDispositivo());
        
        try {
            String ipCliente = obtenerIpCliente(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            SolicitarCodigoResponse response = authDispositivoService.solicitarCodigo(
                request, ipCliente, userAgent
            );
            
            return ResponseEntity.ok(ApiResponse.success(response.getMensaje(), response));
        } catch (Exception e) {
            log.error("Error solicitando código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Verificar código OTP", 
               description = "Verifica el código y registra el dispositivo")
    @PostMapping("/dispositivo/verificar")
    public ResponseEntity<ApiResponse<VerificarCodigoResponse>> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("POST /api/auth/dispositivo/verificar - Tenant: {}", request.getTenantCodigo());
        
        try {
            String ipCliente = obtenerIpCliente(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            VerificarCodigoResponse response = authDispositivoService.verificarCodigo(
                request, ipCliente, userAgent
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dispositivo registrado exitosamente", response));
        } catch (Exception e) {
            log.error("Error verificando código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reenviar código OTP")
    @PostMapping("/dispositivo/reenviar")
    public ResponseEntity<ApiResponse<SolicitarCodigoResponse>> reenviarCodigo(
            @RequestParam String tenantCodigo,
            @RequestParam String nombreDispositivo,
            HttpServletRequest httpRequest) {
        
        log.info("POST /api/auth/dispositivo/reenviar - Tenant: {}", tenantCodigo);
        
        try {
            String ipCliente = obtenerIpCliente(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            SolicitarCodigoResponse response = authDispositivoService.reenviarCodigo(
                tenantCodigo, nombreDispositivo, ipCliente, userAgent
            );
            
            return ResponseEntity.ok(ApiResponse.success("Código reenviado", response));
        } catch (Exception e) {
            log.error("Error reenviando código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== AUTENTICACIÓN CON PIN ====================

    @Operation(summary = "Obtener usuarios del dispositivo", 
               description = "Lista los usuarios disponibles para login en el dispositivo")
    @GetMapping("/dispositivo/usuarios")
    public ResponseEntity<ApiResponse<ObtenerUsuariosResponse>> obtenerUsuarios(
            @Parameter(description = "Token del dispositivo", required = true)
            @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken) {
        
        log.info("GET /api/auth/dispositivo/usuarios");
        
        try {
            ObtenerUsuariosResponse response = authPinService.obtenerUsuarios(deviceToken);
            return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos", response));
        } catch (Exception e) {
            log.error("Error obteniendo usuarios: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Login con PIN", 
               description = "Autenticación de usuario local con PIN de 4-6 dígitos")
    @PostMapping("/pin")
    public ResponseEntity<ApiResponse<LoginPinResponse>> loginConPin(
            @Parameter(description = "Token del dispositivo", required = true)
            @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken,
            @Valid @RequestBody LoginPinRequest request) {
        
        log.info("POST /api/auth/pin - Usuario: {}", request.getUsuarioId());
        
        try {
            LoginPinResponse response = authPinService.loginConPin(deviceToken, request);
            
            String mensaje = response.isRequiereCambioPin() 
                ? "Login exitoso - Cambio de PIN requerido" 
                : "Login exitoso";
            
            return ResponseEntity.ok(ApiResponse.success(mensaje, response));
        } catch (Exception e) {
            log.error("Error en login con PIN: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cambiar PIN", 
               description = "Permite al usuario cambiar su PIN")
    @PostMapping("/pin/cambiar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MensajeResponse>> cambiarPin(
            @Valid @RequestBody CambiarPinRequest request,
            Authentication authentication) {
        
        log.info("POST /api/auth/pin/cambiar");
        
        try {
            Long usuarioId = obtenerUsuarioIdDeAuth(authentication);
            authPinService.cambiarPin(usuarioId, request);
            
            MensajeResponse response = MensajeResponse.builder()
                .mensaje("PIN actualizado correctamente")
                .success(true)
                .build();
            
            return ResponseEntity.ok(ApiResponse.success("PIN cambiado", response));
        } catch (Exception e) {
            log.error("Error cambiando PIN: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== CERRAR SESIÓN ====================

    @Operation(summary = "Cerrar sesión", 
               description = "Cierra la sesión del usuario (el dispositivo sigue registrado)")
    @PostMapping("/cerrar-sesion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MensajeResponse>> cerrarSesion() {
        log.info("POST /api/auth/cerrar-sesion");
        
        // En JWT stateless no hay sesión que cerrar en el servidor
        // El cliente debe descartar el token
        // Opcionalmente se puede implementar una blacklist de tokens
        
        MensajeResponse response = MensajeResponse.builder()
            .mensaje("Sesión cerrada")
            .success(true)
            .build();
        
        return ResponseEntity.ok(ApiResponse.success("Sesión cerrada", response));
    }

    // ==================== HELPERS ====================

    /**
     * Obtiene la IP del cliente considerando proxies
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Extrae el ID del usuario del Authentication
     */
    private Long obtenerUsuarioIdDeAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        // Si es ContextoUsuario
        if (principal instanceof com.snnsoluciones.backnathbitpos.security.ContextoUsuario) {
            return ((com.snnsoluciones.backnathbitpos.security.ContextoUsuario) principal).getUserId();
        }
        
        throw new RuntimeException("No se pudo obtener el ID del usuario");
    }
}
