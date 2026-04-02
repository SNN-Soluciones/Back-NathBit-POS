package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.v2sesion.V2OpcionesReporteDTO;

public interface V2ReporteCajaService {
    String generarHtmlSesion(Long sesionId, V2OpcionesReporteDTO opciones);
    String generarHtmlTurno(Long turnoId, V2OpcionesReporteDTO opciones);
}