package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.metrics.VentaDiariaDTO;
import com.snnsoluciones.backnathbitpos.service.metrics.VentaDiariaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas-diarias")
@RequiredArgsConstructor
@Slf4j
public class VentaDiariaController {

    private final VentaDiariaService ventaDiariaService;

    /**
     * Obtener ventas del día - Empresa
     */
    @GetMapping("/hoy/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerVentasHoyEmpresa(
            @PathVariable Long empresaId) {
        
        log.info("Obteniendo ventas del día para empresa: {}", empresaId);
        
        VentaDiariaDTO ventas = ventaDiariaService.obtenerVentasHoyEmpresa(empresaId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", ventas);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener ventas del día - Sucursal
     */
    @GetMapping("/hoy/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'SUPERVISOR', 'CAJERO')")
    public ResponseEntity<Map<String, Object>> obtenerVentasHoySucursal(
            @PathVariable Long sucursalId) {
        
        log.info("Obteniendo ventas del día para sucursal: {}", sucursalId);
        
        VentaDiariaDTO ventas = ventaDiariaService.obtenerVentasHoySucursal(sucursalId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", ventas);
        
        return ResponseEntity.ok(response);
    }
}