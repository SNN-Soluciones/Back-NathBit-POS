package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.bitacora.*;
import com.snnsoluciones.backnathbitpos.service.FacturaBitacoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para gestión de bitácora de facturación electrónica
 * Permite monitorear el estado del procesamiento de facturas
 */
@Slf4j
@RestController
@RequestMapping("/api/factura-bitacora")
@RequiredArgsConstructor
@Tag(name = "Factura Bitácora", description = "Gestión y monitoreo de procesamiento de facturas electrónicas")
public class FacturaBitacoraController {

    private final FacturaBitacoraService bitacoraService;

    // FacturaBitacoraController.java

    @Operation(summary = "Buscar bitácoras (MVP - simple)")
    @GetMapping("/buscar-simple")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<Page<FacturaBitacoraListResponse>>> buscarSimple(
        @RequestParam(required = false) String busqueda,        // ✅ Clave, consecutivo o cliente
        @RequestParam(required = false) String fechaDesde,      // ✅ Formato: 2025-01-01
        @RequestParam(required = false) String fechaHasta,      // ✅ Formato: 2025-01-31
        @RequestParam(required = false) String tipoDocumento,   // ✅ FE, TE, NC, ND
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            log.info("Búsqueda simple - busqueda: {}, fechaDesde: {}, fechaHasta: {}, tipoDocumento: {}",
                busqueda, fechaDesde, fechaHasta, tipoDocumento);

            Page<FacturaBitacoraListResponse> resultado = bitacoraService.buscarSimple(
                busqueda, fechaDesde, fechaHasta, tipoDocumento, page, size
            );

            return ResponseEntity.ok(ApiResponse.ok("Bitácoras encontradas", resultado));
        } catch (Exception e) {
            log.error("Error en búsqueda simple", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al buscar: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener detalle de bitácora",
        description = "Obtiene información completa de una bitácora incluyendo archivos y mensajes")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<FacturaBitacoraDetailResponse>> obtenerDetalle(
            @PathVariable Long id) {
        
        try {
            log.info("Obteniendo detalle de bitácora ID: {}", id);
            FacturaBitacoraDetailResponse detalle = bitacoraService.obtenerDetalle(id);
            
            return ResponseEntity.ok(
                ApiResponse.ok("Detalle de bitácora", detalle)
            );
        } catch (Exception e) {
            log.error("Error al obtener detalle de bitácora", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener detalle: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reenviar-correo")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'CAJERO')")
    public ResponseEntity<ApiResponse<String>> reenviarCorreo(
        @PathVariable Long id,
        @RequestParam(required = false) String emailDestino) {

        try {
            String mensaje = bitacoraService.reenviarCorreo(id, emailDestino);
            return ResponseEntity.ok(
                ApiResponse.ok(mensaje)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al reenviar: " + e.getMessage()));
        }
    }

    @Operation(summary = "Buscar bitácora por clave",
        description = "Busca una bitácora específica por clave de documento")
    @GetMapping("/clave/{clave}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<FacturaBitacoraDetailResponse>> buscarPorClave(
            @PathVariable String clave) {
        
        try {
            log.info("Buscando bitácora por clave: {}", clave);
            FacturaBitacoraDetailResponse detalle = bitacoraService.buscarPorClave(clave);
            
            return ResponseEntity.ok(
                ApiResponse.ok("Bitácora encontrada", detalle)
            );
        } catch (Exception e) {
            log.error("Error al buscar por clave", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Bitácora no encontrada: " + e.getMessage()));
        }
    }

    @Operation(summary = "Buscar bitácora por factura",
        description = "Obtiene la bitácora asociada a una factura específica")
    @GetMapping("/factura/{facturaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<FacturaBitacoraDetailResponse>> buscarPorFactura(
            @PathVariable Long facturaId) {
        
        try {
            log.info("Buscando bitácora para factura ID: {}", facturaId);
            FacturaBitacoraDetailResponse detalle = bitacoraService.buscarPorFacturaId(facturaId);
            
            return ResponseEntity.ok(
                ApiResponse.ok("Bitácora de factura", detalle)
            );
        } catch (Exception e) {
            log.error("Error al buscar por factura", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Bitácora no encontrada: " + e.getMessage()));
        }
    }

    @Operation(summary = "Reintentar procesamiento",
        description = "Fuerza el reintento manual del procesamiento de una factura")
    @PostMapping("/reintentar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<FacturaBitacoraActionResponse>> reintentarProcesamiento(
            @Valid @RequestBody ReintentarProcesamientoRequest request) {
        
        try {
            log.info("Reintentando procesamiento para bitácora ID: {}", request.getBitacoraId());
            FacturaBitacoraActionResponse respuesta = bitacoraService.reintentarProcesamiento(request);
            
            if (respuesta.isExitoso()) {
                return ResponseEntity.ok(
                    ApiResponse.ok("Reintento programado exitosamente", respuesta)
                );
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(respuesta.getMensaje()));
            }
        } catch (Exception e) {
            log.error("Error al reintentar procesamiento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al reintentar: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener estadísticas",
        description = "Obtiene estadísticas del procesamiento de facturas electrónicas")
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<FacturaBitacoraEstadisticasResponse>> obtenerEstadisticas(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long sucursalId) {
        
        try {
            log.info("Obteniendo estadísticas - Empresa: {}, Sucursal: {}", empresaId, sucursalId);
            FacturaBitacoraEstadisticasResponse stats = bitacoraService.obtenerEstadisticas(empresaId, sucursalId);
            
            return ResponseEntity.ok(
                ApiResponse.ok("Estadísticas de procesamiento", stats)
            );
        } catch (Exception e) {
            log.error("Error al obtener estadísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    @Operation(summary = "Descargar archivo",
        description = "Descarga un archivo específico de la bitácora (XML, PDF, etc)")
    @GetMapping("/{id}/archivo/{tipo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'JEFE_CAJAS')")
    public ResponseEntity<?> descargarArchivo(
            @PathVariable Long id,
            @PathVariable String tipo) {
        
        try {
            log.info("Descargando archivo {} para bitácora ID: {}", tipo, id);
            // Este método retornará el archivo directamente
            return bitacoraService.descargarArchivo(id, tipo);
        } catch (Exception e) {
            log.error("Error al descargar archivo", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Archivo no encontrado: " + e.getMessage()));
        }
    }

    @Operation(summary = "Cancelar procesamiento",
        description = "Cancela el procesamiento de una factura (solo si está pendiente)")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<FacturaBitacoraActionResponse>> cancelarProcesamiento(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        
        try {
            log.info("Cancelando procesamiento de bitácora ID: {}", id);
            FacturaBitacoraActionResponse respuesta = bitacoraService.cancelarProcesamiento(id, motivo);
            
            if (respuesta.isExitoso()) {
                return ResponseEntity.ok(
                    ApiResponse.ok("Procesamiento cancelado", respuesta)
                );
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(respuesta.getMensaje()));
            }
        } catch (Exception e) {
            log.error("Error al cancelar procesamiento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al cancelar: " + e.getMessage()));
        }
    }
}