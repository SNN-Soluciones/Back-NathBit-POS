package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraActionResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraDetailResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraEstadisticasResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraFilterRequest;
import com.snnsoluciones.backnathbitpos.dto.bitacora.FacturaBitacoraListResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.ReintentarProcesamientoRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

/**
 * Servicio para gestión de bitácora de facturación electrónica
 * Maneja consultas, estadísticas y acciones sobre el procesamiento
 */
public interface FacturaBitacoraService {

    /**
     * Busca bitácoras aplicando filtros
     * @param filtros Criterios de búsqueda
     * @return Página de resultados
     */
    Page<FacturaBitacoraListResponse> buscarConFiltros(FacturaBitacoraFilterRequest filtros);

    /**
     * Obtiene el detalle completo de una bitácora
     * @param id ID de la bitácora
     * @return Detalle completo con archivos y mensajes
     */
    FacturaBitacoraDetailResponse obtenerDetalle(Long id);

    /**
     * Busca una bitácora por clave de documento
     * @param clave Clave del documento (50 dígitos)
     * @return Detalle de la bitácora
     */
    FacturaBitacoraDetailResponse buscarPorClave(String clave);

    /**
     * Busca la bitácora asociada a una factura
     * @param facturaId ID de la factura
     * @return Detalle de la bitácora
     */
    FacturaBitacoraDetailResponse buscarPorFacturaId(Long facturaId);

    /**
     * Fuerza el reintento manual del procesamiento
     * @param request Datos del reintento
     * @return Respuesta de la acción
     */
    FacturaBitacoraActionResponse reintentarProcesamiento(ReintentarProcesamientoRequest request);

    /**
     * Cancela el procesamiento de una factura
     * @param bitacoraId ID de la bitácora
     * @param motivo Motivo de la cancelación
     * @return Respuesta de la acción
     */
    FacturaBitacoraActionResponse cancelarProcesamiento(Long bitacoraId, String motivo);

    /**
     * Obtiene estadísticas del procesamiento
     * @param empresaId Filtrar por empresa (opcional)
     * @param sucursalId Filtrar por sucursal (opcional)
     * @return Estadísticas calculadas
     */
    FacturaBitacoraEstadisticasResponse obtenerEstadisticas(Long empresaId, Long sucursalId);

    /**
     * Descarga un archivo asociado a la bitácora
     * @param bitacoraId ID de la bitácora
     * @param tipoArchivo Tipo: xml, xml_firmado, xml_respuesta, pdf
     * @return ResponseEntity con el archivo
     */
    ResponseEntity<?> descargarArchivo(Long bitacoraId, String tipoArchivo);

    String reenviarCorreo(Long bitacoraId, String emailOverride);
}