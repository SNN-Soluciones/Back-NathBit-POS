package com.snnsoluciones.backnathbitpos.controller.admin;

import com.snnsoluciones.backnathbitpos.dto.admin.TenantAdminDTOs.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.repository.global.DispositivoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dispositivos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administración de Dispositivos", description = "Gestión de dispositivos registrados")
public class DispositivoAdminController {

    private final DispositivoRepository dispositivoRepository;
    private final TenantRepository tenantRepository;

    @Operation(summary = "Listar todos los dispositivos (solo ROOT/SOPORTE)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<List<DispositivoDetailResponse>>> listarTodos() {
        log.info("GET /api/admin/dispositivos");
        List<Dispositivo> dispositivos = dispositivoRepository.findAll();
        List<DispositivoDetailResponse> response = dispositivos.stream()
            .map(this::mapToDetailResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Dispositivos obtenidos", response));
    }

    @Operation(summary = "Listar dispositivos de un tenant")
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DispositivoListResponse>>> listarPorTenant(
        @PathVariable Long tenantId,
        @RequestParam(required = false, defaultValue = "false") Boolean soloActivos) {
        log.info("GET /api/admin/dispositivos/tenant/{} - soloActivos: {}", tenantId, soloActivos);
        if (!tenantRepository.existsById(tenantId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Tenant no encontrado: " + tenantId));
        }
        List<Dispositivo> dispositivos = soloActivos
            ? dispositivoRepository.findByTenantIdAndActivoTrue(tenantId)
            : dispositivoRepository.findByTenantId(tenantId);
        List<DispositivoListResponse> response = dispositivos.stream()
            .map(this::mapToListResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Dispositivos obtenidos", response));
    }

    @Operation(summary = "Obtener detalle de un dispositivo")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DispositivoDetailResponse>> obtenerPorId(@PathVariable Long id) {
        log.info("GET /api/admin/dispositivos/{}", id);
        return dispositivoRepository.findById(id)
            .map(dispositivo -> {
                DispositivoDetailResponse response = mapToDetailResponse(dispositivo);
                return ResponseEntity.ok(ApiResponse.success("Dispositivo obtenido", response));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Dispositivo no encontrado: " + id)));
    }

    @Operation(summary = "Desconectar (desactivar) un dispositivo")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> desconectar(@PathVariable Long id) {
        log.info("DELETE /api/admin/dispositivos/{}", id);
        Dispositivo dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dispositivo no encontrado: " + id));
        if (!dispositivo.getActivo()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El dispositivo ya está desconectado"));
        }
        dispositivo.setActivo(false);
        dispositivo.setUpdatedAt(LocalDateTime.now());
        dispositivoRepository.save(dispositivo);
        log.info("Dispositivo {} desconectado", id);
        return ResponseEntity.ok(ApiResponse.success("Dispositivo desconectado",
            "Dispositivo '" + dispositivo.getNombre() + "' desconectado exitosamente"));
    }

    @Operation(summary = "Reactivar un dispositivo")
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> activar(@PathVariable Long id) {
        log.info("PATCH /api/admin/dispositivos/{}/activar", id);
        Dispositivo dispositivo = dispositivoRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dispositivo no encontrado: " + id));
        if (dispositivo.getActivo()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("El dispositivo ya está activo"));
        }
        dispositivo.setActivo(true);
        dispositivo.setUpdatedAt(LocalDateTime.now());
        dispositivoRepository.save(dispositivo);
        log.info("Dispositivo {} reactivado", id);
        return ResponseEntity.ok(ApiResponse.success("Dispositivo reactivado",
            "Dispositivo '" + dispositivo.getNombre() + "' reactivado exitosamente"));
    }

    @Operation(summary = "Desconectar todos los dispositivos de un tenant")
    @DeleteMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    @Transactional
    public ResponseEntity<ApiResponse<String>> desconectarPorTenant(@PathVariable Long tenantId) {
        log.info("DELETE /api/admin/dispositivos/tenant/{}", tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new NotFoundException("Tenant no encontrado: " + tenantId));
        long activos = dispositivoRepository.countByTenantIdAndActivoTrue(tenantId);
        if (activos == 0) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No hay dispositivos activos para desconectar"));
        }
        dispositivoRepository.desactivarPorTenant(tenantId, LocalDateTime.now());
        log.info("{} dispositivos desconectados del tenant {}", activos, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Dispositivos desconectados",
            activos + " dispositivos de '" + tenant.getNombre() + "' desconectados"));
    }

    @Operation(summary = "Buscar dispositivos por nombre en un tenant")
    @GetMapping("/tenant/{tenantId}/buscar")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DispositivoListResponse>>> buscarPorNombre(
        @PathVariable Long tenantId, @RequestParam String nombre) {
        log.info("GET /api/admin/dispositivos/tenant/{}/buscar - nombre: {}", tenantId, nombre);
        List<Dispositivo> dispositivos = dispositivoRepository.buscarPorNombre(tenantId, nombre);
        List<DispositivoListResponse> response = dispositivos.stream()
            .map(this::mapToListResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Dispositivos encontrados", response));
    }

    @Operation(summary = "Obtener estadísticas de dispositivos de un tenant")
    @GetMapping("/tenant/{tenantId}/stats")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DispositivoStats>> obtenerEstadisticas(@PathVariable Long tenantId) {
        log.info("GET /api/admin/dispositivos/tenant/{}/stats", tenantId);
        if (!tenantRepository.existsById(tenantId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Tenant no encontrado: " + tenantId));
        }
        List<Dispositivo> todos = dispositivoRepository.findByTenantId(tenantId);
        long activos = todos.stream().filter(Dispositivo::getActivo).count();
        long web = todos.stream().filter(d -> d.getPlataforma() != null && d.getPlataforma().name().equals("WEB")).count();
        long android = todos.stream().filter(d -> d.getPlataforma() != null && d.getPlataforma().name().equals("ANDROID")).count();
        long ios = todos.stream().filter(d -> d.getPlataforma() != null && d.getPlataforma().name().equals("IOS")).count();
        long windows = todos.stream().filter(d -> d.getPlataforma() != null && d.getPlataforma().name().equals("WINDOWS")).count();

        DispositivoStats stats = DispositivoStats.builder()
            .totalDispositivos((long) todos.size())
            .dispositivosActivos(activos)
            .dispositivosInactivos((long) todos.size() - activos)
            .porPlataformaWeb(web)
            .porPlataformaAndroid(android)
            .porPlataformaIOS(ios)
            .porPlataformaWindows(windows)
            .build();
        return ResponseEntity.ok(ApiResponse.success("Estadísticas obtenidas", stats));
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private DispositivoListResponse mapToListResponse(Dispositivo dispositivo) {
        return DispositivoListResponse.builder()
            .id(dispositivo.getId())
            .nombre(dispositivo.getNombre())
            .plataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma().name() : null)
            .ipRegistro(dispositivo.getIpRegistro())
            .activo(dispositivo.getActivo())
            .ultimoUso(dispositivo.getUltimoUso())
            .createdAt(dispositivo.getCreatedAt())
            .build();
    }

    private DispositivoDetailResponse mapToDetailResponse(Dispositivo dispositivo) {
        Tenant tenant = dispositivo.getTenant();
        String tokenMasked = dispositivo.getToken() != null && dispositivo.getToken().length() > 8
            ? "..." + dispositivo.getToken().substring(dispositivo.getToken().length() - 8)
            : dispositivo.getToken();
        return DispositivoDetailResponse.builder()
            .id(dispositivo.getId())
            .nombre(dispositivo.getNombre())
            .token(tokenMasked)
            .plataforma(dispositivo.getPlataforma() != null ? dispositivo.getPlataforma().name() : null)
            .userAgent(dispositivo.getUserAgent())
            .ipRegistro(dispositivo.getIpRegistro())
            .activo(dispositivo.getActivo())
            .ultimoUso(dispositivo.getUltimoUso())
            .createdAt(dispositivo.getCreatedAt())
            .tenantId(tenant != null ? tenant.getId() : null)
            .tenantCodigo(tenant != null ? tenant.getCodigo() : null)
            .tenantNombre(tenant != null ? tenant.getNombre() : null)
            .build();
    }

    // ==================== DTO INTERNO ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DispositivoStats {
        private Long totalDispositivos;
        private Long dispositivosActivos;
        private Long dispositivosInactivos;
        private Long porPlataformaWeb;
        private Long porPlataformaAndroid;
        private Long porPlataformaIOS;
        private Long porPlataformaWindows;
    }
}