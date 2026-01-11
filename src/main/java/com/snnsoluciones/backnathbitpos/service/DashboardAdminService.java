package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardAdminResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.DashboardEmpresaDetalladoResponse;

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

    /**
     * Obtiene el dashboard detallado de una empresa específica
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
     * Lógica de acceso:
     * - ROOT/SOPORTE: Cualquier empresa
     * - SUPER_ADMIN: Solo empresas asignadas
     *
     * @param usuarioId ID del usuario que solicita el dashboard
     * @param empresaId ID de la empresa a consultar
     * @return Response con toda la información detallada
     */
    DashboardEmpresaDetalladoResponse obtenerDashboardEmpresaDetallado(Long usuarioId, Long empresaId);
}