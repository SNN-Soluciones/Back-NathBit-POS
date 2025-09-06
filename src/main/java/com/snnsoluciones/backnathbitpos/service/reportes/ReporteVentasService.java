package com.snnsoluciones.backnathbitpos.service.reportes;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasResponse;
import net.sf.jasperreports.engine.JRException;
import java.io.IOException;

/**
 * Service para generación de reportes de ventas
 */
public interface ReporteVentasService {
    
    /**
     * Genera reporte de ventas según los parámetros
     * @param request Parámetros del reporte
     * @return Response con datos y archivo generado
     */
    ReporteVentasResponse generarReporteVentas(ReporteVentasRequest request) 
        throws JRException, IOException;
    
    /**
     * Genera solo los datos del reporte sin generar archivo
     * Útil para preview o procesamiento adicional
     */
    ReporteVentasResponse obtenerDatosReporte(ReporteVentasRequest request);
    
    /**
     * Valida que el usuario tenga permisos para ver la sucursal
     */
    void validarAccesoSucursal(Long sucursalId);
}