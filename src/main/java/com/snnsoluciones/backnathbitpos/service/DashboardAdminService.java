package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardAdminResponse;

/**
 * Servicio para gestionar el Dashboard Administrativo
 * Proporciona métricas y estadísticas para usuarios ROOT, SOPORTE y SUPER_ADMIN
 */
public interface DashboardAdminService {
    
    /**
     * Obtiene la lista de empresas con sus ventas del día para el dashboard admin
     * 
     * Lógica de acceso:
     * - ROOT/SOPORTE: Todas las empresas del sistema
     * - SUPER_ADMIN: Solo empresas asignadas en UsuarioEmpresa
     * 
     * @param usuarioId ID del usuario que solicita el dashboard
     * @return Response con lista de empresas y sus ventas de hoy
     */
    DashboardAdminResponse obtenerDashboardAdmin(Long usuarioId);
}