package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoRequest;
import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoResponse;
import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioAsignacionRequest;
import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioListadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.UsuarioAsignacionService;
import com.snnsoluciones.backnathbitpos.service.UsuarioCreacionService;
import com.snnsoluciones.backnathbitpos.service.UsuarioListadoService;
import com.snnsoluciones.backnathbitpos.service.UsuarioPermisosService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios - Creación", description = "Endpoints para creación y gestión de usuarios")
public class UsuarioCreacionController {

  private final UsuarioCreacionService usuarioCreacionService;
  private final UsuarioListadoService usuarioListadoService;
  private final UsuarioPermisosService usuarioPermisosService;
  private final UsuarioService usuarioService;
  private final UsuarioAsignacionService usuarioAsignacionService;

  @Operation(summary = "Crear usuario completo con asignaciones",
      description = "Crea un usuario con todas sus asignaciones en una sola operación")
  @PostMapping("/crear-completo")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<CrearUsuarioCompletoResponse>> crearUsuarioCompleto(
      @Valid @RequestBody CrearUsuarioCompletoRequest request) {

    log.info("=== ENDPOINT: CREAR USUARIO COMPLETO ===");
    log.info("Solicitud para crear usuario con email: {}", request.getEmail());

    try {
      CrearUsuarioCompletoResponse response = usuarioCreacionService.crearUsuarioCompleto(request);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.ok("Usuario creado exitosamente", response));

    } catch (RuntimeException e) {
      log.error("Error de negocio: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error inesperado", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al crear el usuario"));
    }
  }

  @Operation(summary = "Obtener roles que puede crear el usuario actual")
  @GetMapping("/roles-creables")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<RolNombre>>> obtenerRolesCreables(Authentication auth) {

    // Obtener el usuario actual por email (que viene en auth.getName())
    String emailUsuario = auth.getName();
    Usuario usuarioActual = usuarioService.buscarPorEmail(emailUsuario).orElse(null);

    if (usuarioActual == null) {
      log.error("Usuario no encontrado con email: {}", emailUsuario);
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("Usuario no encontrado"));
    }

    List<RolNombre> rolesCreables = usuarioPermisosService.obtenerRolesCreables(
        usuarioActual.getRol());

    log.info("Usuario {} con rol {} puede crear roles: {}",
        emailUsuario, usuarioActual.getRol(), rolesCreables);

    return ResponseEntity.ok(
        ApiResponse.ok("Roles disponibles para crear", rolesCreables)
    );
  }

  @Operation(summary = "Obtener empresas asignables por el usuario actual")
  @GetMapping("/empresas-asignables")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerEmpresasAsignables(
      Authentication auth) {

    String emailUsuario = auth.getName();
    Usuario usuarioActual = usuarioService.buscarPorEmail(emailUsuario).orElse(null);

    if (usuarioActual == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("Usuario no encontrado"));
    }

    List<Empresa> empresas = usuarioPermisosService.obtenerEmpresasAsignables(usuarioActual);

    // Convertir a formato simple para el frontend
    List<Map<String, Object>> empresasResponse = empresas.stream()
        .map(empresa -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", empresa.getId());
          map.put("nombre", empresa.getNombreComercial());
          map.put("identificacion", empresa.getIdentificacion());
          return map;
        })
        .toList();

    return ResponseEntity.ok(
        ApiResponse.ok("Empresas disponibles", empresasResponse)
    );
  }

  @Operation(summary = "Obtener sucursales asignables de una empresa")
  @GetMapping("/sucursales-asignables/{empresaId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerSucursalesAsignables(
      @PathVariable Long empresaId,
      Authentication auth) {

    String emailUsuario = auth.getName();
    Usuario usuarioActual = usuarioService.buscarPorEmail(emailUsuario).orElse(null);

    if (usuarioActual == null) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(ApiResponse.error("Usuario no encontrado"));
    }

    List<Sucursal> sucursales = usuarioPermisosService.obtenerSucursalesAsignables(
        usuarioActual, empresaId);

    // Convertir a formato simple para el frontend
    List<Map<String, Object>> sucursalesResponse = sucursales.stream()
        .map(sucursal -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", sucursal.getId());
          map.put("nombre", sucursal.getNombre());
          map.put("numeroSucursal", sucursal.getNumeroSucursal());
          return map;
        })
        .toList();

    return ResponseEntity.ok(
        ApiResponse.ok("Sucursales disponibles", sucursalesResponse)
    );
  }

  @Operation(summary = "Listar usuarios con filtros según permisos",
      description = "Lista usuarios filtrando por empresa, sucursal y opción de incluir sin asignar")
  @GetMapping("/listar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Page<UsuarioListadoResponse>>> listarUsuarios(
      @RequestParam(required = false) Long empresaId,
      @RequestParam(required = false) Long sucursalId,
      @RequestParam(defaultValue = "true") Boolean incluirSinAsignar,
      Pageable pageable,
      Authentication auth) {

    log.info("=== ENDPOINT: LISTAR USUARIOS ===");
    log.info("Filtros - Empresa: {}, Sucursal: {}, Sin asignar: {}",
        empresaId, sucursalId, incluirSinAsignar);

    try {
      ContextoUsuario contexto = (ContextoUsuario) auth.getPrincipal();
      Long usuarioId = contexto.getUserId();

      Usuario usuarioActual = usuarioService.buscarPorId(usuarioId).orElse(null);

      if (usuarioActual == null) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Usuario no encontrado"));
      }

      // Si es ROOT y no especifica empresa, listar todos
      if (usuarioActual.getRol().equals(RolNombre.ROOT) && empresaId == null) {
        Page<UsuarioListadoResponse> usuarios = usuarioListadoService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Usuarios listados exitosamente", usuarios));
      }

      // Para todos los demás casos, aplicar filtros
      Page<UsuarioListadoResponse> usuarios = usuarioListadoService.listarUsuariosFiltrados(
          usuarioActual,
          empresaId,
          sucursalId,
          incluirSinAsignar,
          pageable
      );

      return ResponseEntity.ok(
          ApiResponse.ok("Usuarios listados exitosamente", usuarios)
      );

    } catch (RuntimeException e) {
      log.error("Error de negocio: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error inesperado al listar usuarios", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error interno al listar usuarios"));
    }
  }
}