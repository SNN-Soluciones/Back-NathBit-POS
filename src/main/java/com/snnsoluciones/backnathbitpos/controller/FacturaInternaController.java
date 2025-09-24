package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.service.FacturaInternaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facturas-internas")
@RequiredArgsConstructor
@Slf4j
public class FacturaInternaController {

    private final FacturaInternaService facturaInternaService;

    /**
     * Crear nueva factura interna
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<FacturaInternaResponse>> crear(
            @Valid @RequestBody CrearFacturaInternaRequest request) {
        log.info("POST /api/facturas-internas - Creando nueva factura interna");
        
        try {
            FacturaInternaResponse factura = facturaInternaService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Factura interna creada exitosamente", factura));
        } catch (Exception e) {
            log.error("Error al crear factura interna: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error al crear factura: " + e.getMessage()));
        }
    }

    /**
     * Buscar factura por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<FacturaInternaResponse>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/facturas-internas/{}", id);
        
        try {
            FacturaInternaResponse factura = facturaInternaService.buscarPorId(id);
            return ResponseEntity.ok(ApiResponse.success("Factura encontrada", factura));
        } catch (Exception e) {
            log.error("Error al buscar factura: ", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Factura no encontrada"));
        }
    }

    /**
     * Listar facturas con filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Page<FacturaInternaListResponse>>> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String estado,
            Pageable pageable) {
        log.info("GET /api/facturas-internas - Listando facturas");
        
        try {
            Page<FacturaInternaListResponse> facturas = facturaInternaService.buscar(
                    empresaId, sucursalId, estado, pageable);
            return ResponseEntity.ok(ApiResponse.success("Facturas encontradas", facturas));
        } catch (Exception e) {
            log.error("Error al listar facturas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener facturas"));
        }
    }

    /**
     * Anular factura
     */
    @PostMapping("/{id}/anular/{usuarioId}")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> anular(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @Valid @RequestBody AnularFacturaRequest request) {
        log.info("POST /api/facturas-internas/{}/anular", id);
        
        try {
            facturaInternaService.anular(id, usuarioId, request);
            return ResponseEntity.ok(ApiResponse.success("Factura anulada exitosamente", null));
        } catch (Exception e) {
            log.error("Error al anular factura: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Error al anular factura: " + e.getMessage()));
        }
    }

    /**
     * Obtener siguiente número de factura
     * Útil para mostrar en UI antes de crear
     */
    @GetMapping("/siguiente-numero")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<String>> siguienteNumero() {
        // TODO: Implementar en el service si es necesario
        return ResponseEntity.ok(ApiResponse.success("Siguiente número", "INT-2024-00001"));
    }
}