package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.metrics.MetricaDashboardDTO;
import com.snnsoluciones.backnathbitpos.dto.metrics.MetricaMensualDTO;
import com.snnsoluciones.backnathbitpos.service.metrics.MetricaMensualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metricas")
@RequiredArgsConstructor
@Slf4j
public class MetricaMensualController {

    private final MetricaMensualService metricaService;

    /**
     * Obtener métricas para dashboard de empresa
     */
    @GetMapping("/dashboard/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerMetricasDashboardEmpresa(
            @PathVariable Long empresaId) {

        log.info("Obteniendo métricas dashboard para empresa: {}", empresaId);

        MetricaDashboardDTO metricas = metricaService.obtenerMetricasDashboard(empresaId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metricas);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener métricas para dashboard de sucursal
     */
    @GetMapping("/dashboard/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'SUPERVISOR', 'CAJERO')")
    public ResponseEntity<Map<String, Object>> obtenerMetricasDashboardSucursal(
            @PathVariable Long sucursalId) {

        log.info("Obteniendo métricas dashboard para sucursal: {}", sucursalId);

        MetricaDashboardDTO metricas = metricaService.obtenerMetricasDashboardSucursal(sucursalId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metricas);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener datos para reporte D104
     */
    @GetMapping("/d104/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerDatosD104(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {

        // Si no se especifica año/mes, usar el actual
        if (anio == null) anio = LocalDate.now().getYear();
        if (mes == null) mes = LocalDate.now().getMonthValue();

        log.info("Obteniendo datos D104 para empresa: {} - {}/{}", empresaId, mes, anio);

        Map<String, Object> datosD104 = metricaService.obtenerDatosD104(empresaId, anio, mes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", datosD104);
        response.put("periodo", String.format("%02d/%d", mes, anio));

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener histórico anual
     */
    @GetMapping("/historico/{empresaId}/{anio}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerHistoricoAnual(
            @PathVariable Long empresaId,
            @PathVariable Integer anio) {

        log.info("Obteniendo histórico anual para empresa: {} - año: {}", empresaId, anio);

        List<MetricaMensualDTO> historico = metricaService.obtenerHistoricoAnual(empresaId, anio);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", historico);
        response.put("total", historico.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener métricas por sucursales
     */
    @GetMapping("/sucursales/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerMetricasPorSucursales(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {

        // Si no se especifica año/mes, usar el actual
        if (anio == null) anio = LocalDate.now().getYear();
        if (mes == null) mes = LocalDate.now().getMonthValue();

        log.info("Obteniendo métricas por sucursales para empresa: {} - {}/{}",
                empresaId, mes, anio);

        List<MetricaMensualDTO> metricasSucursales =
            metricaService.obtenerMetricasPorSucursales(empresaId, anio, mes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metricasSucursales);
        response.put("total", metricasSucursales.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check para verificar si hay métricas
     */
    @GetMapping("/health/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<Map<String, Object>> healthCheck(@PathVariable Long empresaId) {

        LocalDate hoy = LocalDate.now();
        List<MetricaMensualDTO> metricas =
            metricaService.obtenerMetricasPorSucursales(empresaId, hoy.getYear(), hoy.getMonthValue());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", metricas.isEmpty() ?
            "No hay métricas generadas aún" :
            "Sistema de métricas funcionando correctamente");
        response.put("registros", metricas.size());
        response.put("fecha", hoy);

        return ResponseEntity.ok(response);
    }
}