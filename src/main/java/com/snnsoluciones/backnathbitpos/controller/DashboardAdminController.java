package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardAdminResponse;
import com.snnsoluciones.backnathbitpos.service.DashboardAdminService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para Dashboard Administrativo
 * Endpoints para métricas y estadísticas de empresas
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard-admin")
@RequiredArgsConstructor
@Tag(name = "Dashboard Admin", description = "Endpoints para dashboard administrativo")
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;
    private final SecurityContextService securityContextService;

    /**
     * GET /api/dashboard-admin
     *
     * Obtiene lista de empresas con ventas del día
     *
     * Acceso:
     * - ROOT: Todas las empresas del sistema
     * - SOPORTE: Todas las empresas del sistema
     * - SUPER_ADMIN: Solo empresas asignadas
     *
     * @return Lista de empresas con sus ventas de hoy
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(
        summary = "Obtener dashboard administrativo",
        description = "Retorna lista de empresas con sus ventas del día actual. " +
            "ROOT/SOPORTE ven todas las empresas. " +
            "SUPER_ADMIN solo ve empresas asignadas."
    )
    public ResponseEntity<ApiResponse<DashboardAdminResponse>> obtenerDashboard() {
        Long usuarioId = securityContextService.getCurrentUserId();
        log.info("GET /api/dashboard-admin - Usuario: {}", usuarioId);

        DashboardAdminResponse response = dashboardAdminService.obtenerDashboardAdmin(usuarioId);

        return ResponseEntity.ok(
            ApiResponse.<DashboardAdminResponse>builder()
                .success(true)
                .data(response)
                .message("Dashboard obtenido exitosamente")
                .build()
        );
    }
}