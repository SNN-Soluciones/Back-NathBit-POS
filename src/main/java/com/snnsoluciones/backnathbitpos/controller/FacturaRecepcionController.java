package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.facturarecepcion.*;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcion;
import com.snnsoluciones.backnathbitpos.entity.Proveedor;
import com.snnsoluciones.backnathbitpos.enums.factura.EstadoFacturaRecepcion;
import com.snnsoluciones.backnathbitpos.service.FacturaRecepcionService;
import com.snnsoluciones.backnathbitpos.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/facturas-recepcion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Facturas Recepción", description = "Gestión de facturas electrónicas recibidas de proveedores")
public class FacturaRecepcionController {

    private final ModelMapper modelMapper;

    private final FacturaRecepcionService facturaRecepcionService;
    private final ProveedorService proveedorService;


    /**
     * Listar facturas recibidas con filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    @Operation(summary = "Listar facturas recibidas", 
               description = "Lista todas las facturas electrónicas recibidas con filtros opcionales")
    public ResponseEntity<ApiResponse<Page<FacturaRecepcionListResponse>>> listar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) EstadoFacturaRecepcion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Pageable pageable) {
        
        log.info("GET /api/facturas-recepcion - empresa: {}, estado: {}", empresaId, estado);
        
        try {
            Page<FacturaRecepcionListResponse> page = facturaRecepcionService.listar(
                empresaId, sucursalId, estado, fechaInicio, fechaFin, pageable
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("Facturas encontradas: " + page.getTotalElements(), page)
            );
            
        } catch (Exception e) {
            log.error("Error listando facturas", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al listar: " + e.getMessage()));
        }
    }

    /**
     * Obtener detalle completo de factura
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
    @Operation(summary = "Obtener detalle de factura recibida", 
               description = "Obtiene el detalle completo de una factura electrónica recibida")
    public ResponseEntity<ApiResponse<FacturaRecepcionResponse>> obtenerDetalle(
            @PathVariable Long id) {
        
        log.info("GET /api/facturas-recepcion/{}", id);
        
        try {
            FacturaRecepcionResponse response = facturaRecepcionService.obtenerDetalle(id);
            return ResponseEntity.ok(ApiResponse.success("Factura encontrada", response));
            
        } catch (Exception e) {
            log.error("Error obteniendo factura {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Tomar decisión sobre factura (Aceptar/Parcial/Rechazar)
     */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    @Operation(summary = "Tomar decisión sobre factura", 
               description = "Acepta, acepta parcialmente o rechaza una factura recibida. Envía mensaje receptor a Hacienda.")
    public ResponseEntity<ApiResponse<MensajeReceptorResponse>> tomarDecision(
            @PathVariable Long id,
            @Valid @RequestBody DecisionMensajeRequest request) {
        
        log.info("POST /api/facturas-recepcion/{}/decision - decisión: {}", id, request.getDecision());
        
        try {
            MensajeReceptorResponse response = facturaRecepcionService.tomarDecision(id, request);
            return ResponseEntity.ok(
                ApiResponse.success("Decisión procesada exitosamente", response)
            );
            
        } catch (Exception e) {
            log.error("Error procesando decisión para factura {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Convertir factura aceptada a Compra
     */
    @PostMapping("/{id}/convertir-compra")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    @Operation(summary = "Convertir a compra", 
               description = "Convierte una factura recibida ACEPTADA en un registro de Compra y actualiza métricas")
    public ResponseEntity<ApiResponse<ConvertirCompraResponse>> convertirACompra(
            @PathVariable Long id) {
        
        log.info("POST /api/facturas-recepcion/{}/convertir-compra", id);
        
        try {
            ConvertirCompraResponse response = facturaRecepcionService.convertirACompra(id);
            return ResponseEntity.ok(
                ApiResponse.success("Factura convertida a compra exitosamente", response)
            );
            
        } catch (Exception e) {
            log.error("Error convirtiendo factura {} a compra", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Obtener estadísticas de facturas recibidas
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Estadísticas de facturas recibidas", 
               description = "Obtiene estadísticas resumidas de facturas recibidas por empresa/sucursal")
    public ResponseEntity<ApiResponse<Object>> obtenerEstadisticas(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Long sucursalId) {
        
        log.info("GET /api/facturas-recepcion/estadisticas - empresa: {}", empresaId);
        
        // TODO: Implementar si es necesario
        return ResponseEntity.ok(
            ApiResponse.success("Funcionalidad pendiente de implementar", null)
        );
    }

    /**
     * Vincular proveedor a factura
     */
    @PostMapping("/{facturaId}/vincular-proveedor")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<FacturaRecepcion>> vincularProveedor(
        @PathVariable Long facturaId,
        @RequestBody VincularProveedorRequest request) {

        log.info("POST /api/facturas-recepcion/{}/vincular-proveedor - proveedorId: {}",
            facturaId, request.getProveedorId());

        try {
            FacturaRecepcion factura = facturaRecepcionService.buscarPorId(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

            ProveedorDto proveedor = proveedorService.obtenerPorId(request.getProveedorId());
            if (proveedor == null) {
                throw new RuntimeException("Proveedor no encontrado");
            }
            factura.setProveedor(modelMapper.map(proveedor, Proveedor.class));
//            factura = facturaRecepcionService.(factura);

            return ResponseEntity.ok(
                ApiResponse.success("Proveedor vinculado exitosamente", factura)
            );

        } catch (Exception e) {
            log.error("Error vinculando proveedor a factura {}", facturaId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/procesar-email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FacturaRecepcionResponse>> procesarDesdeEmail(
        @RequestParam Long empresaId,
        @RequestParam Long sucursalId,
        @RequestPart("xmlFile") MultipartFile xmlFile,
        @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile) {

        try {
            SubirXmlRequest request = SubirXmlRequest.builder()
                .empresaId(empresaId)
                .sucursalId(sucursalId)
                .xmlFile(xmlFile)
                .pdfFile(pdfFile)
                .crearProveedorSiNoExiste(true)
                .build();

            FacturaRecepcionResponse response = facturaRecepcionService.guardarDesdeEmail(request);

            return ResponseEntity.ok(ApiResponse.success("OK", response));

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(value = "/subir-xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
    @Operation(summary = "Subir XML y procesar automáticamente",
        description = "Sube XML, busca/crea proveedor, acepta y crea compra en un solo paso")
    public ResponseEntity<ApiResponse<FacturaRecepcionResponse>> subirXml(
        @RequestParam Long empresaId,
        @RequestParam Long sucursalId,
        @RequestPart("xmlFile") MultipartFile xmlFile,
        @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
        @RequestParam(defaultValue = "true") boolean crearProveedorSiNoExiste,
        @RequestParam(defaultValue = "true") boolean aceptarAutomaticamente) {

        log.info("POST /api/facturas-recepcion/subir-xml - empresa: {}, sucursal: {}", empresaId, sucursalId);

        try {
            // Construir request usando el DTO
            SubirXmlRequest request = SubirXmlRequest.builder()
                .empresaId(empresaId)
                .sucursalId(sucursalId)
                .xmlFile(xmlFile)
                .pdfFile(pdfFile)
                .crearProveedorSiNoExiste(crearProveedorSiNoExiste)
                .aceptarAutomaticamente(aceptarAutomaticamente)
                .build();

            FacturaRecepcionResponse response = facturaRecepcionService.subirYProcesarCompleto(request);

            String mensaje = response.isConvertidaACompra()
                ? "✅ Factura procesada y compra #" + response.getCompraId() + " registrada exitosamente"
                : "⚠️ Factura guardada pero requiere revisión manual";

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mensaje, response));

        } catch (Exception e) {
            log.error("Error procesando XML", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error procesando factura: " + e.getMessage()));
        }
    }

    /**
     * Genera un reporte Excel de facturas aceptadas en un rango de fechas
     *
     * GET /api/facturas-recepcion/reporte-excel?fechaInicio=2025-01-01&fechaFin=2025-01-31
     *
     * @param fechaInicio Fecha de inicio del rango (formato: yyyy-MM-dd)
     * @param fechaFin Fecha de fin del rango (formato: yyyy-MM-dd)
     * @return Archivo Excel descargable
     */
    @GetMapping("/reporte-excel")
    public ResponseEntity<byte[]> generarReporteExcel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        log.info("🚀 Generando reporte Excel - Rango: {} a {}", fechaInicio, fechaFin);

        // Validaciones
        if (fechaInicio.isAfter(fechaFin)) {
            log.warn("❌ Fecha inicio es posterior a fecha fin");
            return ResponseEntity.badRequest().build();
        }

        // Validar rango no mayor a 1 año (opcional, por performance)
        if (fechaInicio.plusYears(1).isBefore(fechaFin)) {
            log.warn("❌ Rango de fechas excede 1 año");
            return ResponseEntity.badRequest().build();
        }

        try {
            // Generar Excel
            byte[] excelBytes = facturaRecepcionService.generarReporteExcel(fechaInicio, fechaFin);

            // Nombre del archivo
            String filename = String.format("Facturas_Aceptadas_%s_%s.xlsx",
                fechaInicio.format(DateTimeFormatter.BASIC_ISO_DATE),
                fechaFin.format(DateTimeFormatter.BASIC_ISO_DATE)
            );

            // Headers HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            log.info("✅ Reporte Excel generado exitosamente: {} bytes", excelBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);

        } catch (Exception e) {
            log.error("❌ Error generando reporte Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO
    @Data
    public static class VincularProveedorRequest {
        private Long proveedorId;
    }
}