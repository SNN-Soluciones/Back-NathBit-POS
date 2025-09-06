package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controller para reportes de ventas
 * "Great Scott!" - Doc Brown
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes/ventas")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Endpoints para generación de reportes")
public class ReporteVentasController {
    
    private final ReporteVentasService reporteVentasService;
    
    @Operation(summary = "Generar reporte de ventas",
        description = "Genera reporte de ventas (+Facturas +Tiquetes -Notas) con totales")
    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<?> generarReporte(@Valid @RequestBody ReporteVentasRequest request) {
        try {
            log.info("Generando reporte de ventas para sucursal: {}", request.getSucursalId());
            
            ReporteVentasResponse response = reporteVentasService.generarReporteVentas(request);
            
            // Si es descarga directa de archivo
            if (response.getArchivoGenerado() != null) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + response.getNombreArchivo() + "\"")
                    .contentType(MediaType.parseMediaType(response.getTipoContenido()))
                    .body(response.getArchivoGenerado());
            }
            
            // Si es preview de datos
            return ResponseEntity.ok(ApiResponse.ok(
                "Reporte generado exitosamente", 
                response
            ));
            
        } catch (JRException e) {
            log.error("Error generando reporte Jasper", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error generando reporte: " + e.getMessage()));
        } catch (IOException e) {
            log.error("Error de I/O generando reporte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al generar archivo: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado generando reporte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error inesperado: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Obtener preview de datos",
        description = "Obtiene los datos del reporte sin generar archivo")
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<ReporteVentasResponse>> previewReporte(
            @Valid @RequestBody ReporteVentasRequest request) {
        
        try {
            ReporteVentasResponse datos = reporteVentasService.obtenerDatosReporte(request);
            
            return ResponseEntity.ok(ApiResponse.ok(
                String.format("Se encontraron %d documentos", datos.getTotalDocumentos()),
                datos
            ));
            
        } catch (Exception e) {
            log.error("Error obteniendo datos del reporte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener datos: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Descargar reporte de ventas",
        description = "Genera y descarga directamente el archivo del reporte")
    @PostMapping("/descargar")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<?> descargarReporte(@Valid @RequestBody ReporteVentasRequest request) {
        try {
            ReporteVentasResponse response = reporteVentasService.generarReporteVentas(request);
            
            if (response.getArchivoGenerado() == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.error("No se pudo generar el archivo"));
            }
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + response.getNombreArchivo() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.getArchivoGenerado().length))
                .contentType(MediaType.parseMediaType(response.getTipoContenido()))
                .body(response.getArchivoGenerado());
                
        } catch (Exception e) {
            log.error("Error descargando reporte", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al descargar: " + e.getMessage()));
        }
    }
}