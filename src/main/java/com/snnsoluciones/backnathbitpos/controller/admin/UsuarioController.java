package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.security.Permiso;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de usuarios
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "API para la gestión de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

  private final UsuarioService usuarioService;

  /**
   * Crear un nuevo usuario
   */
  @PostMapping("/usuarios")
  @PreAuthorize("hasAuthority('USUARIO_CREAR')")
  @Operation(summary = "Crear un nuevo usuario", description = "Crea un nuevo usuario en el sistema")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El email ya existe")
  })
  public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
      @Valid @RequestBody UsuarioCreateRequest request) {

    log.info("Solicitud para crear nuevo usuario con email: {}", request.getEmail());

    UsuarioResponse usuario = usuarioService.crear(request);

    return new ResponseEntity<>(
        ApiResponse.<UsuarioResponse>builder()
            .success(true)
            .message("Usuario creado exitosamente")
            .data(usuario)
            .build(),
        HttpStatus.CREATED
    );
  }

  /**
   * Actualizar un usuario existente
   */
  @PutMapping("/usuarios/{id}")
  @PreAuthorize("hasAuthority('USUARIO_ACTUALIZAR')")
  @Operation(summary = "Actualizar un usuario", description = "Actualiza los datos de un usuario existente")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
      @Parameter(description = "ID del usuario") @PathVariable UUID id,
      @Valid @RequestBody UsuarioUpdateRequest request) {

    log.info("Solicitud para actualizar usuario con ID: {}", id);

    UsuarioResponse usuario = usuarioService.actualizar(id, request);

    return ResponseEntity.ok(
        ApiResponse.<UsuarioResponse>builder()
            .success(true)
            .message("Usuario actualizado exitosamente")
            .data(usuario)
            .build()
    );
  }

  /**
   * Obtener un usuario por ID
   */
  @GetMapping("/usuarios/{id}")
  @PreAuthorize("hasAuthority('USUARIO_VER')")
  @Operation(summary = "Obtener usuario por ID", description = "Obtiene los detalles de un usuario específico")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario encontrado"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerPorId(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para obtener usuario con ID: {}", id);

    UsuarioResponse usuario = usuarioService.obtenerPorId(id);

    return ResponseEntity.ok(
        ApiResponse.<UsuarioResponse>builder()
            .success(true)
            .message("Usuario encontrado")
            .data(usuario)
            .build()
    );
  }

  /**
   * Listar usuarios con paginación
   */
  @GetMapping("/usuarios")
  @PreAuthorize("hasAuthority('USUARIO_VER')")
  @Operation(summary = "Listar usuarios", description = "Obtiene un listado paginado de usuarios")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Listado obtenido exitosamente")
  })
  public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
      @Parameter(description = "Parámetros de paginación")
      @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {

    log.info("Solicitud para listar usuarios - Página: {}, Tamaño: {}",
        pageable.getPageNumber(), pageable.getPageSize());

    Page<UsuarioResponse> usuarios = usuarioService.listar(pageable);

    return ResponseEntity.ok(
        ApiResponse.<Page<UsuarioResponse>>builder()
            .success(true)
            .message("Listado obtenido exitosamente")
            .data(usuarios)
            .build()
    );
  }

  /**
   * Eliminar un usuario (soft delete)
   */
  @DeleteMapping("/usuarios/{id}")
  @PreAuthorize("hasAuthority('USUARIO_ELIMINAR')")
  @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema (soft delete)")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<Void>> eliminar(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para eliminar usuario con ID: {}", id);

    usuarioService.eliminar(id);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Usuario eliminado exitosamente")
            .build()
    );
  }

  /**
   * Cambiar contraseña de un usuario
   */
  @PutMapping("/usuarios/{id}/password")
  @PreAuthorize("hasAuthority('USUARIO_ACTUALIZAR') or #id == authentication.principal.id")
  @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña de un usuario")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Contraseña inválida o incorrecta"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<Void>> cambiarPassword(
      @Parameter(description = "ID del usuario") @PathVariable UUID id,
      @Valid @RequestBody CambioPasswordRequest request) {

    log.info("Solicitud para cambiar contraseña del usuario con ID: {}", id);

    usuarioService.cambiarPassword(id, request);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Contraseña cambiada exitosamente")
            .build()
    );
  }

  /**
   * Bloquear un usuario
   */
  @PutMapping("/usuarios/{id}/bloquear")
  @PreAuthorize("hasAuthority('USUARIO_BLOQUEAR')")
  @Operation(summary = "Bloquear usuario", description = "Bloquea el acceso de un usuario al sistema")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario bloqueado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<Void>> bloquear(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para bloquear usuario con ID: {}", id);

    usuarioService.bloquearUsuario(id);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Usuario bloqueado exitosamente")
            .build()
    );
  }

  /**
   * Desbloquear un usuario
   */
  @PutMapping("/usuarios/{id}/desbloquear")
  @PreAuthorize("hasAuthority('USUARIO_BLOQUEAR')")
  @Operation(summary = "Desbloquear usuario", description = "Desbloquea el acceso de un usuario al sistema")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario desbloqueado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  })
  public ResponseEntity<ApiResponse<Void>> desbloquear(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para desbloquear usuario con ID: {}", id);

    usuarioService.desbloquearUsuario(id);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Usuario desbloqueado exitosamente")
            .build()
    );
  }

  /**
   * Obtener información del usuario autenticado
   */
  @GetMapping("/me")
  @Operation(summary = "Mi perfil", description = "Obtiene la información del usuario autenticado actual")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Información obtenida exitosamente")
  })
  public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPerfilActual(
      @AuthenticationPrincipal Usuario usuario,
      Authentication authentication) {

    log.info("Solicitud para obtener perfil del usuario actual: {}", usuario.getEmail());

    // Obtener la información completa del usuario
    UsuarioResponse usuarioResponse = usuarioService.obtenerPorId(usuario.getId());

    // Construir la respuesta con información adicional
    Map<String, Object> perfil = new HashMap<>();
    perfil.put("usuario", usuarioResponse);

    // Agregar rol y permisos
    Map<String, Object> seguridad = new HashMap<>();

    // Manejar el rol único del usuario
    if (usuario.getRol() != null) {
      seguridad.put("rol", Map.of(
          "id", usuario.getRol().getId(),
          "nombre", usuario.getRol().getNombre().name(),
          "descripcion", usuario.getRol().getDescripcion() != null ? usuario.getRol().getDescripcion() : ""
      ));

      // Obtener permisos del rol
      if (usuario.getRol().getPermisos() != null) {
        seguridad.put("permisos", usuario.getRol().getPermisos().stream()
            .map(permiso -> Map.of(
                "id", permiso.getId(),
                "codigo", permiso.getCodigo(),
                "nombre", permiso.getNombre(),
                "descripcion", permiso.getDescripcion() != null ? permiso.getDescripcion() : ""
            ))
            .collect(Collectors.toList())
        );
      }
    }

    perfil.put("seguridad", seguridad);

    // Agregar sucursales con información detallada
    if (usuario.getSucursales() != null && !usuario.getSucursales().isEmpty()) {
      perfil.put("sucursales", usuario.getSucursales().stream()
          .map(sucursal -> {
            Map<String, Object> sucursalInfo = new HashMap<>();
            sucursalInfo.put("id", sucursal.getId());
            sucursalInfo.put("nombre", sucursal.getNombre());
            sucursalInfo.put("codigo", sucursal.getCodigo());
            sucursalInfo.put("activa", sucursal.getActivo());
            sucursalInfo.put("esPrincipal", sucursal.getEsPrincipal());
            return sucursalInfo;
          })
          .collect(Collectors.toList())
      );
    }

    // Información adicional del contexto
    Map<String, Object> contexto = new HashMap<>();
    contexto.put("tenantId", usuario.getTenantId());
    contexto.put("ultimoAcceso", usuario.getUltimoAcceso());
    contexto.put("autenticado", authentication.isAuthenticated());
    contexto.put("authorities", authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList())
    );

    perfil.put("contexto", contexto);

    return ResponseEntity.ok(
        ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .message("Perfil obtenido exitosamente")
            .data(perfil)
            .build()
    );
  }
}