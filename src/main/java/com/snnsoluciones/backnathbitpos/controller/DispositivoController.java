package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.dispositivo.*;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.DispositivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de dispositivos PDV
 * <p>
 * Endpoints públicos (no requieren autenticación): - POST /api/dispositivos/registrar - Registrar
 * nuevo PDV con token
 * <p>
 * Endpoints con device token (header X-Device-Token): - GET /api/dispositivos/usuarios - Lista
 * usuarios del dispositivo
 * <p>
 * Endpoints admin (requieren JWT): - POST /api/admin/dispositivos/generar-token - Genera token de
 * registro - GET /api/admin/dispositivos/empresa/{empresaId} - Lista dispositivos - PUT
 * /api/admin/dispositivos/{id}/activar - Activa dispositivo - PUT
 * /api/admin/dispositivos/{id}/desactivar - Desactiva dispositivo
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dispositivos PDV", description = "Gestión de dispositivos punto de venta")
public class DispositivoController {

  private final DispositivoService dispositivoService;
  private final UsuarioRepository usuarioRepository;

  private static final String HEADER_DEVICE_TOKEN = "X-Device-Token";

  // ==================== ENDPOINTS PÚBLICOS ====================

  @Operation(summary = "Registrar dispositivo PDV",
      description = "Registra un nuevo dispositivo usando un token generado por el admin")
  @PostMapping("/dispositivos/registrar")
  public ResponseEntity<ApiResponse<RegistrarDispositivoResponse>> registrarDispositivo(
      @Valid @RequestBody RegistrarDispositivoRequest request,
      HttpServletRequest httpRequest) {

    log.info("POST /api/dispositivos/registrar - Token: {}", request.getToken());

    try {
      String ipCliente = obtenerIpCliente(httpRequest);
      RegistrarDispositivoResponse response = dispositivoService.registrarDispositivo(request,
          ipCliente);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Dispositivo registrado exitosamente", response));
    } catch (Exception e) {
      log.error("Error registrando dispositivo: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  // ==================== ENDPOINTS CON DEVICE TOKEN ====================

  @Operation(summary = "Obtener usuarios del dispositivo",
      description = "Lista los usuarios disponibles para login en el dispositivo")
  @GetMapping("/dispositivos/usuarios")
  public ResponseEntity<ApiResponse<DispositivoUsuariosResponse>> obtenerUsuarios(
      @Parameter(description = "Token del dispositivo", required = true)
      @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken,
      @RequestParam(required = false, defaultValue = "false") boolean includeRoot) {

    log.info("GET /api/dispositivos/usuarios");

    try {
      DispositivoUsuariosResponse response = dispositivoService.obtenerUsuariosDispositivo(
          deviceToken, includeRoot);
      return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos", response));
    } catch (Exception e) {
      log.error("Error obteniendo usuarios: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  // ==================== ENDPOINTS ADMIN ====================

  @Operation(summary = "Generar token de registro",
      description = "Genera un token temporal para registrar un nuevo PDV (Admin Web)")
  @PostMapping("/admin/dispositivos/generar-token")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<GenerarTokenResponse>> generarToken(
      @Valid @RequestBody GenerarTokenRequest request) {

    log.info("POST /api/admin/dispositivos/generar-token - Empresa: {}, Sucursal: {}",
        request.getEmpresaId(), request.getSucursalId());

    try {
      GenerarTokenResponse response = dispositivoService.generarTokenRegistro(request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success("Token generado exitosamente", response));
    } catch (Exception e) {
      log.error("Error generando token: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @Operation(summary = "Listar dispositivos de una empresa",
      description = "Obtiene todos los dispositivos registrados de una empresa")
  @GetMapping("/admin/dispositivos/empresa/{empresaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<DispositivoDTO>>> listarDispositivos(
      @PathVariable Long empresaId) {

    log.info("GET /api/admin/dispositivos/empresa/{}", empresaId);

    try {
      List<DispositivoDTO> dispositivos = dispositivoService.listarDispositivosPorEmpresa(
          empresaId);
      return ResponseEntity.ok(ApiResponse.success("Dispositivos obtenidos", dispositivos));
    } catch (Exception e) {
      log.error("Error listando dispositivos: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @Operation(summary = "Activar dispositivo",
      description = "Activa un dispositivo previamente desactivado")
  @PutMapping("/admin/dispositivos/{id}/activar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> activarDispositivo(@PathVariable Long id) {

    log.info("PUT /api/admin/dispositivos/{}/activar", id);

    try {
      dispositivoService.activarDispositivo(id);
      return ResponseEntity.ok(ApiResponse.success("Dispositivo activado", null));
    } catch (Exception e) {
      log.error("Error activando dispositivo: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @Operation(summary = "Desactivar dispositivo",
      description = "Desactiva un dispositivo bloqueando su acceso al sistema")
  @PutMapping("/admin/dispositivos/{id}/desactivar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> desactivarDispositivo(@PathVariable Long id) {

    log.info("PUT /api/admin/dispositivos/{}/desactivar", id);

    try {
      dispositivoService.desactivarDispositivo(id);
      return ResponseEntity.ok(ApiResponse.success("Dispositivo desactivado", null));
    } catch (Exception e) {
      log.error("Error desactivando dispositivo: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @Operation(summary = "Listar sucursales de una empresa (simplificado)",
      description = "Retorna lista simple de sucursales para selección en registro de PDV")
  @GetMapping("/admin/dispositivos/empresa/{empresaId}/sucursales")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<SucursalSimpleDTO>>> listarSucursales(
      @PathVariable Long empresaId) {

    log.info("GET /api/admin/dispositivos/empresa/{}/sucursales", empresaId);

    try {
      List<SucursalSimpleDTO> sucursales = dispositivoService.listarSucursalesPorEmpresa(empresaId);
      return ResponseEntity.ok(ApiResponse.success("Sucursales obtenidas", sucursales));
    } catch (Exception e) {
      log.error("Error listando sucursales: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @PutMapping("/admin/usuarios/{id}/resetear-pin")
  @PreAuthorize("hasRole('ROOT')")
  public ResponseEntity<ApiResponse<Void>> resetearPin(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {

    Usuario usuario = usuarioRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    String nuevoPin = (String) request.get("nuevoPin");
    Boolean requiereCambio = (Boolean) request.getOrDefault("requiereCambio", true);

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    usuario.setPin(encoder.encode(nuevoPin));
    usuario.setRequiereCambioPin(requiereCambio);

    usuarioRepository.save(usuario);

    return ResponseEntity.ok(ApiResponse.success("PIN reseteado exitosamente", null));
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
}