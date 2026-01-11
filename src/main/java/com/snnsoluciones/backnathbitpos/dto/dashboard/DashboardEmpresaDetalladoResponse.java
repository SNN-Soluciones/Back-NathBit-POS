package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.util.List;

/**
 * Response completo del endpoint GET /api/dashboard-admin-empresa/{empresaId}
 * Contiene toda la información detallada de la empresa
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEmpresaDetalladoResponse {
    
    /**
     * Datos básicos de la empresa
     */
    private EmpresaBasicaDTO empresa;
    
    /**
     * Métricas generales (ventas, cajas, usuarios)
     */
    private MetricasDTO metricas;
    
    /**
     * Lista de sucursales con sus métricas
     */
    private List<SucursalMetricasDTO> sucursales;
    
    /**
     * Ventas diarias de los últimos 7 días
     */
    private List<VentaDiariaDTO> ventasPorDia;
    
    /**
     * Top 3-5 productos más vendidos hoy
     */
    private List<ProductoTopDTO> topProductos;
    
    /**
     * Lista de usuarios de la empresa
     */
    private List<UsuarioSimpleDTO> usuarios;
    
    /**
     * Lista de cajas actualmente abiertas
     */
    private List<CajaAbiertaDTO> cajas;
}