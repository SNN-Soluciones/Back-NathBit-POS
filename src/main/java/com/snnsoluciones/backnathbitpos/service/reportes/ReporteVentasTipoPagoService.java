package com.snnsoluciones.backnathbitpos.service.reportes;

import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import net.sf.jasperreports.engine.JRException;

public interface ReporteVentasTipoPagoService {

  ReporteVentasTipoPagoResponse generarReporte(ReporteVentasTipoPagoRequest request)
      throws JRException;

  ReporteVentasTipoPagoResponse obtenerDatosReporte(ReporteVentasTipoPagoRequest request);

  void validarAccesoSucursal(Long sucursalId);
}