package com.snnsoluciones.backnathbitpos.service.reportes;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasResponse;

/**
 * Contrato del servicio de reporte de IVA por tarifa en ventas.
 *
 * <p>Genera el desglose de IVA (0%, 1%, 2%, 4%, 8%, 13%) para facturas,
 * tiquetes y notas de crédito de una sucursal en un período determinado,
 * filtrando por el estado del comprobante en la bitácora de Hacienda.</p>
 */
public interface ReporteIvaVentasService {

    /**
     * Genera el reporte de IVA en formato Excel (.xlsx).
     *
     * @param request parámetros de filtro (sucursalId obligatorio)
     * @return bytes del archivo .xlsx
     */
    byte[] generarExcel(ReporteIvaVentasRequest request);

    /**
     * Genera el reporte de IVA por tarifa aplicando todos los filtros del request.
     *
     * <p>Si algún filtro viene nulo se aplican los valores por defecto definidos
     * en {@link ReporteIvaVentasRequest#aplicarDefaults()}.</p>
     *
     * @param request parámetros de filtro (sucursalId obligatorio, resto opcionales)
     * @return response con metadatos, totales y lista de documentos
     */
    ReporteIvaVentasResponse generarReporte(ReporteIvaVentasRequest request);

    /**
     * Valida que la sucursal exista en base de datos.
     *
     * @param sucursalId ID de la sucursal
     * @throws jakarta.persistence.EntityNotFoundException si no existe
     */
    void validarSucursal(Long sucursalId);
}