package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasProDTO;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasProService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/reportes/ventas-pro")
@RequiredArgsConstructor
@Tag(name = "Reporte Ventas Pro", description = "Reporte avanzado de ventas con comparativos y desglose")
public class ReporteVentasProController {

    private final ReporteVentasProService reporteVentasProService;

    @Operation(summary = "Generar reporte de ventas pro",
        description = "Si sucursalId es null → consolidado empresa. "
            + "fechaDesde y fechaHasta usan el día comercial (4am-3:59am).")
    @GetMapping
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<ApiResponse<ReporteVentasProDTO>> generar(
        @RequestParam Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        log.info("Reporte ventas pro — empresa: {}, sucursal: {}, rango: {} a {}",
            empresaId, sucursalId, fechaDesde, fechaHasta);

        if (fechaDesde.isAfter(fechaHasta)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("fechaDesde no puede ser posterior a fechaHasta"));
        }

        ReporteVentasProDTO reporte = reporteVentasProService
            .generar(empresaId, sucursalId, fechaDesde, fechaHasta);

        return ResponseEntity.ok(ApiResponse.ok("Reporte generado exitosamente", reporte));
    }
}