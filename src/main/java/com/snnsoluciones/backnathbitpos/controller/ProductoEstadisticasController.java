package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.estadisticas.ProductoEstadisticasV2Dto;
import com.snnsoluciones.backnathbitpos.service.ProductoEstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/productos/estadisticas")
@RequiredArgsConstructor
@Tag(name = "Productos - Estadísticas", description = "Endpoints para estadísticas de productos")
public class ProductoEstadisticasController {
    
    private final ProductoEstadisticasService estadisticasService;
    
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    @Operation(
        summary = "Obtener estadísticas por empresa",
        description = "Obtiene estadísticas completas de productos a nivel empresa"
    )
    public ResponseEntity<ApiResponse<ProductoEstadisticasV2Dto>> obtenerEstadisticasEmpresa(
            @PathVariable Long empresaId,
            @Parameter(description = "Período: HOY, SEMANA, MES, TOTAL")
            @RequestParam(defaultValue = "TOTAL") String periodo) {
        
        log.info("Solicitando estadísticas para empresa: {}, período: {}", empresaId, periodo);
        
        try {
            ProductoEstadisticasV2Dto estadisticas = estadisticasService
                .obtenerEstadisticasEmpresa(empresaId, periodo.toUpperCase());
                
            return ResponseEntity.ok(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                .success(true)
                .message("Estadísticas obtenidas exitosamente")
                .data(estadisticas)
                .build());
                
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de empresa {}: {}", empresaId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                    .success(false)
                    .message("Error al obtener estadísticas: " + e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    @Operation(
        summary = "Obtener estadísticas por sucursal",
        description = "Obtiene estadísticas completas de productos a nivel sucursal"
    )
    public ResponseEntity<ApiResponse<ProductoEstadisticasV2Dto>> obtenerEstadisticasSucursal(
            @PathVariable Long sucursalId,
            @Parameter(description = "Período: HOY, SEMANA, MES, TOTAL")
            @RequestParam(defaultValue = "TOTAL") String periodo) {
        
        log.info("Solicitando estadísticas para sucursal: {}, período: {}", sucursalId, periodo);
        
        try {
            ProductoEstadisticasV2Dto estadisticas = estadisticasService
                .obtenerEstadisticasSucursal(sucursalId, periodo.toUpperCase());
                
            return ResponseEntity.ok(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                .success(true)
                .message("Estadísticas obtenidas exitosamente")
                .data(estadisticas)
                .build());
                
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de sucursal {}: {}", sucursalId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                    .success(false)
                    .message("Error al obtener estadísticas: " + e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/resumen-rapido/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    @Operation(
        summary = "Resumen rápido de productos",
        description = "Obtiene solo los totales por tipo de producto (para dashboards)"
    )
    public ResponseEntity<ApiResponse<ProductoEstadisticasV2Dto>> obtenerResumenRapido(
            @PathVariable Long empresaId,
            @RequestParam(required = false) Long sucursalId) {
        
        log.info("Solicitando resumen rápido - empresa: {}, sucursal: {}", empresaId, sucursalId);
        
        try {
            ProductoEstadisticasV2Dto estadisticas;
            
            if (sucursalId != null) {
                estadisticas = estadisticasService.obtenerEstadisticasSucursal(sucursalId, "TOTAL");
            } else {
                estadisticas = estadisticasService.obtenerEstadisticasEmpresa(empresaId, "TOTAL");
            }
            
            // Limpiar datos que no son necesarios para el resumen
            estadisticas.setTopCategorias(null);
            estadisticas.setTopProductosVendidos(null);
            
            return ResponseEntity.ok(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                .success(true)
                .message("Resumen obtenido exitosamente")
                .data(estadisticas)
                .build());
                
        } catch (Exception e) {
            log.error("Error obteniendo resumen rápido: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoEstadisticasV2Dto>builder()
                    .success(false)
                    .message("Error al obtener resumen: " + e.getMessage())
                    .build());
        }
    }
}