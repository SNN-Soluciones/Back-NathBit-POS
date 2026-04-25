package com.snnsoluciones.backnathbitpos.controller.dashboard;
 
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial.*;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.dashboard.DashboardEmpresarialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDate;
 
@RestController
@RequestMapping("/api/dashboard/empresarial")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard Empresarial", description = "Métricas y KPIs para SUPER_ADMIN - Multi-Tenant")
public class DashboardEmpresarialController {
 
    private final DashboardEmpresarialService dashboardService;

    // -- 0. resumen resumen
    @GetMapping("/empresas-resumen")
    @PreAuthorize("hasAnyRole('ROOT','SOPORTE','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmpresasResumenResponse>> empresasResumen(Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.empresasResumen(uid)));
    }

    // ── 1. RESUMEN GENERAL (KPIs) ─────────────────────────────────────────
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "KPIs generales del período")
    public ResponseEntity<ApiResponse<ResumenEmpresarialResponse>> resumen(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        Authentication auth) {

        Long usuarioGlobalId = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        log.info("GET /dashboard/empresarial/resumen - usuario={}, desde={}, hasta={}", usuarioGlobalId, fechaDesde, fechaHasta);

        ResumenEmpresarialResponse data = dashboardService.resumen(usuarioGlobalId, fechaDesde, fechaHasta, empresaId);
        return ResponseEntity.ok(ApiResponse.ok("Resumen obtenido", data));
    }

    // ── 2. SERIE TEMPORAL ─────────────────────────────────────────────────
    @GetMapping("/ventas-serie")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Serie temporal de ventas para gráfico de líneas")
    public ResponseEntity<ApiResponse<VentasSerieResponse>> ventasSerie(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        @RequestParam(required = false, defaultValue = "auto") String agruparPor,
        Authentication auth) {

        Long usuarioGlobalId = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        log.info("GET /dashboard/empresarial/ventas-serie - usuario={}", usuarioGlobalId);

        VentasSerieResponse data = dashboardService.ventasSerie(usuarioGlobalId, fechaDesde, fechaHasta, empresaId, sucursalId, agruparPor);
        return ResponseEntity.ok(ApiResponse.ok("Serie temporal obtenida", data));
    }

    // ── 3. VENTAS POR EMPRESA ─────────────────────────────────────────────
    @GetMapping("/ventas-por-empresa")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Comparativo de ventas por empresa")
    public ResponseEntity<ApiResponse<VentasPorEmpresaResponse>> ventasPorEmpresa(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Authentication auth) {

        Long usuarioGlobalId = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        log.info("GET /dashboard/empresarial/ventas-por-empresa - usuario={}", usuarioGlobalId);

        VentasPorEmpresaResponse data = dashboardService.ventasPorEmpresa(usuarioGlobalId, fechaDesde, fechaHasta);
        return ResponseEntity.ok(ApiResponse.ok("Ventas por empresa obtenidas", data));
    }

    @GetMapping("/tipo-pago")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Distribución de ventas por medio de pago")
    public ResponseEntity<ApiResponse<TipoPagoResponse>> tipoPago(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Tipos de pago obtenidos",
            dashboardService.tipoPago(uid, fechaDesde, fechaHasta, empresaId, sucursalId)));
    }

    // ── 5. TOP SUCURSALES ─────────────────────────────────────────────────
    @GetMapping("/top-sucursales")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Ranking de sucursales por ventas")
    public ResponseEntity<ApiResponse<TopSucursalesResponse>> topSucursales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(defaultValue = "10") int limit,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Top sucursales obtenido",
            dashboardService.topSucursales(uid, fechaDesde, fechaHasta, Math.min(limit, 20))));
    }

    // ── 6. TIEMPO REAL ────────────────────────────────────────────────────
    @GetMapping("/tiempo-real")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Estado actual de operaciones (polling 60s)")
    public ResponseEntity<ApiResponse<TiempoRealResponse>> tiempoReal(Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Estado actual obtenido",
            dashboardService.tiempoReal(uid)));
    }

    // ── 7. TOP PRODUCTOS ──────────────────────────────────────────────────
    @GetMapping("/top-productos")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Productos más vendidos cross-empresa")
    public ResponseEntity<ApiResponse<TopProductosResponse>> topProductos(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(defaultValue = "10") int limit,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Top productos obtenido",
            dashboardService.topProductos(uid, fechaDesde, fechaHasta, empresaId, Math.min(limit, 50))));
    }

    // ── 8. ALERTAS ────────────────────────────────────────────────────────
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Alertas operacionales activas (polling 60s)")
    public ResponseEntity<ApiResponse<AlertasResponse>> alertas(Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Alertas obtenidas",
            dashboardService.alertas(uid)));
    }

    // ── 9. HORAS PICO ─────────────────────────────────────────────────────
    @GetMapping("/horas-pico")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Distribución de ventas por hora del día")
    public ResponseEntity<ApiResponse<HorasPicoResponse>> horasPico(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Horas pico obtenidas",
            dashboardService.horasPico(uid, fechaDesde, fechaHasta, empresaId, sucursalId)));
    }

    // ── 10. RENDIMIENTO EMPRESAS ──────────────────────────────────────────
    @GetMapping("/rendimiento-empresas")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Tabla comparativa ejecutiva por empresa")
    public ResponseEntity<ApiResponse<RendimientoEmpresasResponse>> rendimientoEmpresas(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Rendimiento obtenido",
            dashboardService.rendimientoEmpresas(uid, fechaDesde, fechaHasta)));
    }

    // ── 11. IMPUESTOS (IVA) ───────────────────────────────────────────────
    @GetMapping("/impuestos")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Desglose de IVA por tarifa (solo facturas electrónicas)")
    public ResponseEntity<ApiResponse<ImpuestosResponse>> impuestos(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Impuestos obtenidos",
            dashboardService.impuestos(uid, fechaDesde, fechaHasta, empresaId)));
    }

    // ── 12. IMPUESTO DE SERVICIO ──────────────────────────────────────────
    @GetMapping("/impuesto-servicio")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    @Operation(summary = "Impuesto de servicio 10% cobrado (solo facturas electrónicas)")
    public ResponseEntity<ApiResponse<ImpuestoServicioResponse>> impuestoServicio(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required = false) Long empresaId,
        Authentication auth) {
        Long uid = ((ContextoUsuario) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Impuesto de servicio obtenido",
            dashboardService.impuestoServicio(uid, fechaDesde, fechaHasta, empresaId)));
    }
}