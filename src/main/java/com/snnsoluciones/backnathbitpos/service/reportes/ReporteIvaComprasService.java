package com.snnsoluciones.backnathbitpos.service.reportes;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasResponse;

public interface ReporteIvaComprasService {

    /**
     * Genera el reporte de IVA por tarifa en compras (JSON).
     *
     * @param request filtros (sucursalId obligatorio, resto opcionales)
     * @return response con metadatos, totales y lista de documentos
     */
    ReporteIvaComprasResponse generarReporte(ReporteIvaComprasRequest request);

    /**
     * Genera el reporte de IVA en compras como archivo Excel (.xlsx).
     *
     * @param request mismos filtros que generarReporte()
     * @return bytes del archivo .xlsx
     */
    byte[] generarExcel(ReporteIvaComprasRequest request);

    /**
     * Valida que la sucursal exista.
     *
     * @throws jakarta.persistence.EntityNotFoundException si no existe
     */
    void validarSucursal(Long sucursalId);
}