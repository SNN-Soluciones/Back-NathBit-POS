package com.snnsoluciones.backnathbitpos.service.dashboard;
 
import com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial.*;
import java.time.LocalDate;
 
public interface DashboardEmpresarialService {
    EmpresasResumenResponse     empresasResumen(Long uid);
    ResumenEmpresarialResponse   resumen(Long usuarioGlobalId, LocalDate desde, LocalDate hasta, Long empresaId);
    VentasSerieResponse          ventasSerie(Long usuarioGlobalId, LocalDate desde, LocalDate hasta, Long empresaId, Long sucursalId, String agruparPor);
    VentasPorEmpresaResponse     ventasPorEmpresa(Long usuarioGlobalId, LocalDate desde, LocalDate hasta);
    TipoPagoResponse         tipoPago(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, Long sucursalId);
    TopSucursalesResponse    topSucursales(Long uid, LocalDate desde, LocalDate hasta, int limit);
    TiempoRealResponse       tiempoReal(Long uid);
    TopProductosResponse     topProductos(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, int limit);
    AlertasResponse          alertas(Long uid);
    HorasPicoResponse        horasPico(Long uid, LocalDate desde, LocalDate hasta, Long empresaId, Long sucursalId);
    RendimientoEmpresasResponse rendimientoEmpresas(Long uid, LocalDate desde, LocalDate hasta);
    ImpuestosResponse        impuestos(Long uid, LocalDate desde, LocalDate hasta, Long empresaId);
    ImpuestoServicioResponse impuestoServicio(Long uid, LocalDate desde, LocalDate hasta, Long empresaId);
}