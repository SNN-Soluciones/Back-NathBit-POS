package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.dto.admin.TenantAdminDTOs.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.SuperAdminTenant;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.SuperAdminTenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import com.snnsoluciones.backnathbitpos.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administración de Tenants", description = "CRUD de tenants para ROOT y SOPORTE")
@PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
public class TenantAdminController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final DispositivoRepository dispositivoRepository;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Listar todos los tenants")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantListResponse>>> listarTodos() {
        log.info("GET /api/admin/tenants");
        List<Tenant> tenants = tenantService.listarTodos();
        List<TenantListResponse> response = tenants.stream()
            .map(this::mapToListResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Tenants obtenidos", response));
    }

    @Operation(summary = "Listar tenants activos")
    @GetMapping("/activos")
    public ResponseEntity<ApiResponse<List<TenantListResponse>>> listarActivos() {
        log.info("GET /api/admin/tenants/activos");
        List<Tenant> tenants = tenantService.listarActivos();
        List<TenantListResponse> response = tenants.stream()
            .map(this::mapToListResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Tenants activos obtenidos", response));
    }

    @Operation(summary = "Obtener detalle de un tenant")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/admin/tenants/{}", id);
        try {
            Tenant tenant = tenantService.buscarPorId(id);
            TenantDetailResponse response = mapToDetailResponse(tenant);
            return ResponseEntity.ok(ApiResponse.success("Tenant obtenido", response));
        } catch (Exception e) {
            log.error("Error obteniendo tenant {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Buscar tenant por código")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> obtenerPorCodigo(@PathVariable String codigo) {
        log.info("GET /api/admin/tenants/codigo/{}", codigo);
        return tenantService.buscarPorCodigo(codigo)
            .map(tenant -> {
                TenantDetailResponse response = mapToDetailResponse(tenant);
                return ResponseEntity.ok(ApiResponse.success("Tenant obtenido", response));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Tenant no encontrado con código: " + codigo)));
    }

    @Operation(summary = "Crear nuevo tenant")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantDetailResponse>> crear(
            @Valid @RequestBody CreateTenantRequest request) {
        log.info("POST /api/admin/tenants - Razón social: {}", request.getRazonSocial());
        try {
            String codigo = request.getCodigo();
            if (codigo == null || codigo.isBlank()) {
                codigo = tenantService.generarCodigo(request.getRazonSocial());
            }
            Tenant tenant = tenantService.crearTenant(
                request.getRazonSocial(),
                codigo,
                request.getEmpresaLegacyId()
            );
            if (request.getSuperAdminEmail() != null && !request.getSuperAdminEmail().isBlank()) {
                crearYAsignarSuperAdmin(tenant, request);
            }
            TenantDetailResponse response = mapToDetailResponse(tenant);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tenant creado exitosamente", response));
        } catch (Exception e) {
            log.error("Error creando tenant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar tenant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        log.info("PUT /api/admin/tenants/{}", id);
        try {
            Tenant tenant = tenantService.actualizar(id, request.getNombre());
            TenantDetailResponse response = mapToDetailResponse(tenant);
            return ResponseEntity.ok(ApiResponse.success("Tenant actualizado", response));
        } catch (Exception e) {
            log.error("Error actualizando tenant {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Activar tenant")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<String>> activar(@PathVariable Long id) {
        log.info("PATCH /api/admin/tenants/{}/activar", id);
        try {
            tenantService.activar(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant activado", "Tenant " + id + " activado"));
        } catch (Exception e) {
            log.error("Error activando tenant {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Desactivar tenant")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<String>> desactivar(@PathVariable Long id) {
        log.info("PATCH /api/admin/tenants/{}/desactivar", id);
        try {
            tenantService.desactivar(id);
            return ResponseEntity.ok(ApiResponse.success("Tenant desactivado", "Tenant " + id + " desactivado"));
        } catch (Exception e) {
            log.error("Error desactivando tenant {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Asignar SUPER_ADMIN a tenant")
    @PostMapping("/{id}/super-admins")
    public ResponseEntity<ApiResponse<SuperAdminInfo>> asignarSuperAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AsignarSuperAdminRequest request) {
        log.info("POST /api/admin/tenants/{}/super-admins - Usuario: {}", id, request.getUsuarioId());
        try {
            SuperAdminTenant relacion;
            if (Boolean.TRUE.equals(request.getEsPropietario())) {
                relacion = tenantService.asignarPropietario(id, request.getUsuarioId());
            } else {
                relacion = tenantService.asignarColaborador(id, request.getUsuarioId());
            }
            SuperAdminInfo response = mapToSuperAdminInfo(relacion);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SUPER_ADMIN asignado", response));
        } catch (Exception e) {
            log.error("Error asignando SUPER_ADMIN: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Desasignar SUPER_ADMIN de tenant")
    @DeleteMapping("/{id}/super-admins/{usuarioId}")
    public ResponseEntity<ApiResponse<String>> desasignarSuperAdmin(
            @PathVariable Long id, @PathVariable Long usuarioId) {
        log.info("DELETE /api/admin/tenants/{}/super-admins/{}", id, usuarioId);
        try {
            tenantService.desasignarUsuario(id, usuarioId);
            return ResponseEntity.ok(ApiResponse.success("SUPER_ADMIN desasignado", 
                "Usuario " + usuarioId + " desasignado del tenant " + id));
        } catch (Exception e) {
            log.error("Error desasignando SUPER_ADMIN: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Listar SUPER_ADMINs de un tenant")
    @GetMapping("/{id}/super-admins")
    public ResponseEntity<ApiResponse<List<SuperAdminInfo>>> listarSuperAdmins(@PathVariable Long id) {
        log.info("GET /api/admin/tenants/{}/super-admins", id);
        List<SuperAdminTenant> relaciones = superAdminTenantRepository.findByTenantIdAndActivoTrue(id);
        List<SuperAdminInfo> response = relaciones.stream()
            .map(this::mapToSuperAdminInfo)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("SUPER_ADMINs obtenidos", response));
    }

    @Operation(summary = "Obtener estadísticas globales")
    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<EstadisticasGlobales>> obtenerEstadisticas() {
        log.info("GET /api/admin/tenants/estadisticas");
        EstadisticasGlobales stats = EstadisticasGlobales.builder()
            .totalTenants(tenantRepository.count())
            .tenantsActivos(tenantRepository.countByActivoTrue())
            .totalDispositivos(dispositivoRepository.count())
            .dispositivosActivos(dispositivoRepository.findAll().stream()
                .filter(Dispositivo::getActivo).count())
            .totalUsuariosGlobales(usuarioGlobalRepository.count())
            .totalSuperAdmins(usuarioGlobalRepository.countByRol(UsuarioGlobal.RolGlobal.SUPER_ADMIN))
            .build();
        return ResponseEntity.ok(ApiResponse.success("Estadísticas obtenidas", stats));
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private void crearYAsignarSuperAdmin(Tenant tenant, CreateTenantRequest request) {
        if (usuarioGlobalRepository.existsByEmail(request.getSuperAdminEmail())) {
            UsuarioGlobal usuario = usuarioGlobalRepository.findByEmail(request.getSuperAdminEmail())
                .orElseThrow();
            tenantService.asignarPropietario(tenant.getId(), usuario.getId());
        } else {
            UsuarioGlobal nuevoUsuario = UsuarioGlobal.builder()
                .email(request.getSuperAdminEmail())
                .nombre(request.getSuperAdminNombre() != null ? request.getSuperAdminNombre() : "Admin")
                .password(passwordEncoder.encode(request.getSuperAdminPassword()))
                .rol(UsuarioGlobal.RolGlobal.SUPER_ADMIN)
                .activo(true)
                .requiereCambioPassword(true)
                .build();
            nuevoUsuario = usuarioGlobalRepository.save(nuevoUsuario);
            tenantService.asignarPropietario(tenant.getId(), nuevoUsuario.getId());
        }
    }

    private TenantListResponse mapToListResponse(Tenant tenant) {
        return TenantListResponse.builder()
            .id(tenant.getId())
            .codigo(tenant.getCodigo())
            .nombre(tenant.getNombre())
            .schemaName(tenant.getSchemaName())
            .activo(tenant.getActivo())
            .dispositivosActivos(dispositivoRepository.countByTenantIdAndActivoTrue(tenant.getId()))
            .superAdminsAsignados(superAdminTenantRepository.countByTenantIdAndActivoTrue(tenant.getId()))
            .createdAt(tenant.getCreatedAt())
            .build();
    }

    private TenantDetailResponse mapToDetailResponse(Tenant tenant) {
        List<SuperAdminTenant> relaciones = superAdminTenantRepository.findByTenantIdAndActivoTrue(tenant.getId());
        List<SuperAdminInfo> superAdmins = relaciones.stream()
            .map(this::mapToSuperAdminInfo)
            .collect(Collectors.toList());
        return TenantDetailResponse.builder()
            .id(tenant.getId())
            .codigo(tenant.getCodigo())
            .nombre(tenant.getNombre())
            .schemaName(tenant.getSchemaName())
            .empresaLegacyId(tenant.getEmpresaLegacyId())
            .activo(tenant.getActivo())
            .createdAt(tenant.getCreatedAt())
            .updatedAt(tenant.getUpdatedAt())
            .dispositivosActivos(dispositivoRepository.countByTenantIdAndActivoTrue(tenant.getId()))
            .dispositivosTotales((long) dispositivoRepository.findByTenantId(tenant.getId()).size())
            .superAdminsAsignados((long) superAdmins.size())
            .superAdmins(superAdmins)
            .build();
    }

    private SuperAdminInfo mapToSuperAdminInfo(SuperAdminTenant relacion) {
        UsuarioGlobal usuario = relacion.getUsuario();
        return SuperAdminInfo.builder()
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .esPropietario(relacion.getEsPropietario())
            .activo(relacion.getActivo())
            .build();
    }
}