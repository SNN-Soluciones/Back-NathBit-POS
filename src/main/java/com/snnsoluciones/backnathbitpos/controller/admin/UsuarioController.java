package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.request.AsignarSucursalesRequest;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.EmpresaSucursal;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import com.snnsoluciones.backnathbitpos.util.ContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * Controlador REST para la gestión de usuarios en el sistema multi-empresa
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "API para la gestión de usuarios multi-empresa")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

  private final UsuarioService usuarioService;

  /**
   * Crear un nuevo usuario
   */
  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_CREAR')")
  @Operation(summary = "Crear un nuevo usuario",
      description = "Crea un nuevo usuario y lo asigna a una empresa con rol específico")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
          description = "Usuario creado exitosamente"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
          description = "Datos inválidos"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
          description = "El email ya existe")
  })
  public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
      @Valid @RequestBody UsuarioCreateRequest request,
      @AuthenticationPrincipal UsuarioGlobal usuarioActual) {

    log.info("Solicitud para crear nuevo usuario con email: {} para empresa: {}",
        request.getEmail(), request.getEmpresaId());

    // Validar que el usuario actual tiene acceso a la empresa
    UUID empresaActual = ContextUtils.getCurrentEmpresaId();
    if (!request.getEmpresaId().equals(empresaActual) && !isUserSuperAdmin(usuarioActual)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.<UsuarioResponse>builder()
              .success(false)
              .message("No tiene permisos para crear usuarios en esta empresa")
              .build());
    }

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
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_ACTUALIZAR')")
  @Operation(summary = "Actualizar un usuario",
      description = "Actualiza los datos de un usuario existente")
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
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS') or hasAuthority('USUARIO_VER')")
  @Operation(summary = "Obtener usuario por ID",
      description = "Obtiene los detalles de un usuario específico con contexto de empresa actual")
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
   * Lista solo usuarios de la empresa/sucursal actual según el contexto
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS') or hasAuthority('USUARIO_VER')")
  @Operation(summary = "Listar usuarios",
      description = "Obtiene un listado paginado de usuarios de la empresa/sucursal actual")
  public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
      @Parameter(description = "Filtrar por rol") @RequestParam(required = false) String rol,
      @Parameter(description = "Filtrar por sucursal") @RequestParam(required = false) UUID sucursalId,
      @Parameter(description = "Buscar por nombre o email") @RequestParam(required = false) String search,
      @Parameter(description = "Incluir usuarios inactivos") @RequestParam(defaultValue = "false") boolean incluirInactivos,
      @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {

    log.info("Solicitud para listar usuarios - Página: {}, Tamaño: {}",
        pageable.getPageNumber(), pageable.getPageSize());

    Page<UsuarioResponse> usuarios = usuarioService.listar(
        rol, sucursalId, search, incluirInactivos, pageable);

    return ResponseEntity.ok(
        ApiResponse.<Page<UsuarioResponse>>builder()
            .success(true)
            .message("Listado obtenido exitosamente")
            .data(usuarios)
            .build()
    );
  }

  /**
   * Desactivar un usuario (soft delete)
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_ELIMINAR')")
  @Operation(summary = "Desactivar usuario",
      description = "Desactiva un usuario del sistema (soft delete)")
  public ResponseEntity<ApiResponse<Void>> desactivar(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para desactivar usuario con ID: {}", id);

    usuarioService.desactivar(id);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Usuario desactivado exitosamente")
            .build()
    );
  }

  /**
   * Reactivar un usuario
   */
  @PutMapping("/{id}/activar")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_ACTUALIZAR')")
  @Operation(summary = "Reactivar usuario",
      description = "Reactiva un usuario previamente desactivado")
  public ResponseEntity<ApiResponse<Void>> reactivar(
      @Parameter(description = "ID del usuario") @PathVariable UUID id) {

    log.info("Solicitud para reactivar usuario con ID: {}", id);

    usuarioService.reactivar(id);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Usuario reactivado exitosamente")
            .build()
    );
  }

  /**
   * Cambiar contraseña de un usuario
   */
  @PutMapping("/{id}/password")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or #id == authentication.principal.id")
  @Operation(summary = "Cambiar contraseña",
      description = "Cambia la contraseña de un usuario")
  public ResponseEntity<ApiResponse<Void>> cambiarPassword(
      @Parameter(description = "ID del usuario") @PathVariable UUID id,
      @Valid @RequestBody CambioPasswordRequest request,
      @AuthenticationPrincipal UsuarioGlobal usuarioActual) {

    log.info("Solicitud para cambiar contraseña del usuario con ID: {}", id);

    // Validar que es el mismo usuario o tiene permisos
    if (!id.equals(usuarioActual.getId()) && !isUserAdmin(usuarioActual)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.<Void>builder()
              .success(false)
              .message("No tiene permisos para cambiar esta contraseña")
              .build());
    }

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
  @PutMapping("/{id}/bloquear")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_BLOQUEAR')")
  @Operation(summary = "Bloquear usuario",
      description = "Bloquea el acceso de un usuario al sistema")
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
  @PutMapping("/{id}/desbloquear")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('USUARIO_BLOQUEAR')")
  @Operation(summary = "Desbloquear usuario",
      description = "Desbloquea el acceso de un usuario al sistema")
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
  @Operation(summary = "Mi perfil",
      description = "Obtiene la información del usuario autenticado actual con contexto multi-empresa")
  public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPerfilActual(
      @AuthenticationPrincipal UsuarioGlobal usuario,
      Authentication authentication) {

    log.info("Solicitud para obtener perfil del usuario actual: {}", usuario.getEmail());

    Map<String, Object> perfil = new HashMap<>();

    // Información básica del usuario
    Map<String, Object> infoUsuario = new HashMap<>();
    infoUsuario.put("id", usuario.getId());
    infoUsuario.put("email", usuario.getEmail());
    infoUsuario.put("nombre", usuario.getNombre());
    infoUsuario.put("apellidos", usuario.getApellidos());
    infoUsuario.put("telefono", usuario.getTelefono());
    infoUsuario.put("activo", usuario.getActivo());
    infoUsuario.put("ultimoAcceso", usuario.getUltimoAcceso());
    perfil.put("usuario", infoUsuario);

    // Contexto actual (empresa y sucursal)
    Map<String, Object> contextoActual = new HashMap<>();
    UUID empresaId = ContextUtils.getCurrentEmpresaId();
    UUID sucursalId = ContextUtils.getCurrentSucursalId();

    if (empresaId != null) {
      // Buscar información de la empresa actual
      UsuarioEmpresa accesoEmpresa = usuario.getUsuarioEmpresas().stream()
          .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
          .findFirst()
          .orElse(null);

      if (accesoEmpresa != null) {
        contextoActual.put("empresaId", empresaId);
        contextoActual.put("empresaNombre", accesoEmpresa.getEmpresa().getNombre());
        contextoActual.put("empresaCodigo", accesoEmpresa.getEmpresa().getCodigo());
        contextoActual.put("rol", accesoEmpresa.getRol().name());
        contextoActual.put("esPropietario", accesoEmpresa.getEsPropietario());

        // Información de sucursal si está en contexto
        if (sucursalId != null) {
          UsuarioSucursal accesoSucursal = accesoEmpresa.getUsuarioSucursales().stream()
              .filter(us -> us.getSucursal().getId().equals(sucursalId))
              .findFirst()
              .orElse(null);

          if (accesoSucursal != null) {
            contextoActual.put("sucursalId", sucursalId);
            contextoActual.put("sucursalNombre", accesoSucursal.getSucursal().getNombreSucursal());
            contextoActual.put("sucursalCodigo", accesoSucursal.getSucursal().getCodigoSucursal());
            contextoActual.put("permisosSucursal", Map.of(
                "puedeLeer", accesoSucursal.getPuedeLeer(),
                "puedeEscribir", accesoSucursal.getPuedeEscribir(),
                "puedeEliminar", accesoSucursal.getPuedeEliminar(),
                "puedeAprobar", accesoSucursal.getPuedeAprobar()
            ));
          }
        }
      }
    }
    contextoActual.put("tenantId", TenantContext.getCurrentTenant());
    perfil.put("contextoActual", contextoActual);

    // Empresas disponibles
    Map<String, Object> accesos = new HashMap<>();
    accesos.put("cantidadEmpresas", usuario.getUsuarioEmpresas().size());
    accesos.put("empresas", usuario.getUsuarioEmpresas().stream()
        .filter(UsuarioEmpresa::getActivo)
        .map(ue -> {
          Map<String, Object> empresa = new HashMap<>();
          empresa.put("id", ue.getEmpresa().getId());
          empresa.put("codigo", ue.getEmpresa().getCodigo());
          empresa.put("nombre", ue.getEmpresa().getNombre());
          empresa.put("rol", ue.getRol().name());
          empresa.put("esPropietario", ue.getEsPropietario());
          empresa.put("cantidadSucursales", ue.getUsuarioSucursales().size());
          return empresa;
        })
        .collect(Collectors.toList())
    );
    perfil.put("accesos", accesos);

    // Authorities del contexto actual
    Map<String, Object> seguridad = new HashMap<>();
    seguridad.put("authorities", authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList())
    );
    seguridad.put("autenticado", authentication.isAuthenticated());
    perfil.put("seguridad", seguridad);

    return ResponseEntity.ok(
        ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .message("Perfil obtenido exitosamente")
            .data(perfil)
            .build()
    );
  }

  /**
   * Asignar usuario a sucursales adicionales
   */
  @PostMapping("/{id}/sucursales")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Asignar sucursales",
      description = "Asigna un usuario a sucursales adicionales de la empresa")
  public ResponseEntity<ApiResponse<Void>> asignarSucursales(
      @PathVariable UUID id,
      @RequestBody @Valid AsignarSucursalesRequest request) {

    log.info("Asignando usuario {} a {} sucursales", id, request.getSucursalesIds().size());

    usuarioService.asignarSucursales(id, request);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Sucursales asignadas exitosamente")
            .build()
    );
  }

  /**
   * Cambiar rol de usuario en la empresa actual
   */
  @PutMapping("/{id}/rol")
  @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and @usuarioService.canManageUser(#id))")
  @Operation(summary = "Cambiar rol",
      description = "Cambia el rol de un usuario en la empresa actual")
  public ResponseEntity<ApiResponse<Void>> cambiarRol(
      @PathVariable UUID id,
      @RequestParam String nuevoRol) {

    log.info("Cambiando rol de usuario {} a {}", id, nuevoRol);

    usuarioService.cambiarRol(id, nuevoRol);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Rol actualizado exitosamente")
            .build()
    );
  }

  // Métodos helper privados
  private boolean isUserSuperAdmin(UsuarioGlobal usuario) {
    UUID empresaId = ContextUtils.getCurrentEmpresaId();
    return usuario.getUsuarioEmpresas().stream()
        .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId)
            && ue.getRol().name().equals("SUPER_ADMIN"));
  }

  private boolean isUserAdmin(UsuarioGlobal usuario) {
    UUID empresaId = ContextUtils.getCurrentEmpresaId();
    return usuario.getUsuarioEmpresas().stream()
        .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId)
            && (ue.getRol().name().equals("SUPER_ADMIN")
            || ue.getRol().name().equals("ADMIN")));
  }
}