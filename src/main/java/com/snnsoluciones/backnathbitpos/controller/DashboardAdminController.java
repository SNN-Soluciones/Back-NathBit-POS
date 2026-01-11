package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardAdminResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardEmpresaDetalladoResponse;
import com.snnsoluciones.backnathbitpos.service.DashboardAdminService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    
    /**
     * GET /api/dashboard-admin-empresa/{empresaId}
     * 
     * Obtiene dashboard detallado de una empresa específica
     * 
     * Incluye:
     * - Métricas de ventas (hoy, semana, mes)
     * - Cajas abiertas
     * - PDVs activos  
     * - Usuarios activos
     * - Sucursales con métricas
     * - Ventas por día (últimos 7 días)
     * - Top productos vendidos hoy
     * - Lista de usuarios
     * - Lista de cajas abiertas
     * 
     * Acceso:
     * - ROOT/SOPORTE: Cualquier empresa
     * - SUPER_ADMIN: Solo empresas asignadas
     * 
     * @param empresaId ID de la empresa
     * @return Dashboard detallado de la empresa
     */
    @GetMapping("-empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(
        summary = "Obtener dashboard detallado de empresa",
        description = "Retorna información completa de una empresa: métricas, sucursales, " +
                      "ventas por día, top productos, usuarios y cajas abiertas. " +
                      "ROOT/SOPORTE acceden a cualquier empresa. " +
                      "SUPER_ADMIN solo a empresas asignadas."
    )
    public ResponseEntity<ApiResponse<DashboardEmpresaDetalladoResponse>> obtenerDashboardEmpresa(
        @Parameter(description = "ID de la empresa", required = true)
        @PathVariable Long empresaId
    ) {
        Long usuarioId = securityContextService.getCurrentUserId();
        log.info("GET /api/dashboard-admin-empresa/{} - Usuario: {}", empresaId, usuarioId);
        
        DashboardEmpresaDetalladoResponse response = dashboardAdminService
            .obtenerDashboardEmpresaDetallado(usuarioId, empresaId);
        
        return ResponseEntity.ok(
            ApiResponse.<DashboardEmpresaDetalladoResponse>builder()
                .success(true)
                .data(response)
                .message("Dashboard de empresa obtenido exitosamente")
                .build()
        );
    }
}