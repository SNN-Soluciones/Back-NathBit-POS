package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.ProductoVendidoDTO;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteProductosVendidosRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteProductosVendidosResponse;
import com.snnsoluciones.backnathbitpos.service.ReporteProductosVendidosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes/productos-vendidos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reportes de Productos Vendidos", description = "Estadísticas de productos vendidos")
public class ReporteProductosVendidosController {

    private final ReporteProductosVendidosService reporteService;

    /**
     * Top N productos más vendidos en un rango de fechas
     */
    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    @Operation(summary = "Top productos más vendidos", 
               description = "Obtiene los N productos más vendidos en un rango de fechas")
    public ResponseEntity<ApiResponse<List<ProductoVendidoDTO>>> getTopProductos(
            @RequestParam Long sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "10") Integer top) {
        
        log.info("GET /api/reportes/productos-vendidos/top - Sucursal: {}, Top: {}", sucursalId, top);
        
        List<ProductoVendidoDTO> productos = reporteService.getTopProductos(
            sucursalId, fechaDesde, fechaHasta, top
        );
        
        return ResponseEntity.ok(
            ApiResponse.success("Top productos obtenidos exitosamente", productos)
        );
    }

    /**
     * Reporte completo con totales por mes
     */
    @PostMapping("/reporte-mensual")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    @Operation(summary = "Reporte mensual de productos vendidos", 
               description = "Genera reporte agrupado por mes con los productos más vendidos")
    public ResponseEntity<ApiResponse<ReporteProductosVendidosResponse>> getReporteMensual(
            @Valid @RequestBody ReporteProductosVendidosRequest request) {
        
        log.info("POST /api/reportes/productos-vendidos/reporte-mensual - Sucursal: {}", 
            request.getSucursalId());
        
        ReporteProductosVendidosResponse reporte = reporteService.generarReporteMensual(request);
        
        return ResponseEntity.ok(
            ApiResponse.success("Reporte generado exitosamente", reporte)
        );
    }

    /**
     * Reporte diario - productos vendidos hoy
     */
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    @Operation(summary = "Productos vendidos hoy", 
               description = "Obtiene los productos vendidos en el día actual")
    public ResponseEntity<ApiResponse<List<ProductoVendidoDTO>>> getProductosHoy(
            @RequestParam Long sucursalId,
            @RequestParam(defaultValue = "20") Integer top) {
        
        log.info("GET /api/reportes/productos-vendidos/hoy - Sucursal: {}", sucursalId);
        
        List<ProductoVendidoDTO> productos = reporteService.getProductosVendidosHoy(sucursalId, top);
        
        return ResponseEntity.ok(
            ApiResponse.success("Productos del día obtenidos exitosamente", productos)
        );
    }

    /**
     * Detalle de un producto específico en un rango
     */
    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    @Operation(summary = "Detalle de ventas de un producto", 
               description = "Obtiene el histórico de ventas de un producto específico")
    public ResponseEntity<ApiResponse<ProductoVendidoDTO>> getDetalleProducto(
            @PathVariable Long productoId,
            @RequestParam Long sucursalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        
        log.info("GET /api/reportes/productos-vendidos/producto/{} - Sucursal: {}", 
            productoId, sucursalId);
        
        ProductoVendidoDTO detalle = reporteService.getDetalleProducto(
            productoId, sucursalId, fechaDesde, fechaHasta
        );
        
        return ResponseEntity.ok(
            ApiResponse.success("Detalle obtenido exitosamente", detalle)
        );
    }

    /**
     * Comparativa: Este mes vs Mes anterior
     */
    @GetMapping("/comparativa")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    @Operation(summary = "Comparativa de ventas", 
               description = "Compara las ventas del mes actual vs el mes anterior")
    public ResponseEntity<ApiResponse<?>> getComparativa(
            @RequestParam Long sucursalId,
            @RequestParam(defaultValue = "10") Integer top) {
        
        log.info("GET /api/reportes/productos-vendidos/comparativa - Sucursal: {}", sucursalId);
        
        var comparativa = reporteService.getComparativaMensual(sucursalId, top);
        
        return ResponseEntity.ok(
            ApiResponse.success("Comparativa obtenida exitosamente", comparativa)
        );
    }
}