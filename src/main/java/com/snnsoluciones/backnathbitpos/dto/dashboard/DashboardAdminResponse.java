package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.util.List;

/**
 * Response del endpoint GET /api/dashboard-admin
 * Retorna lista de empresas con sus ventas del día
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAdminResponse {
    
    /**
     * Lista de empresas con sus datos y ventas del día
     */
    private List<EmpresaResumenDashboard> empresas;
}