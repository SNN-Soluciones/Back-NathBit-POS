package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.FacturaInternaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
     * Anular factura
     * Acepta tanto /anular como /anular/{usuarioId}
     * Si viene "undefined", lo ignora y usa el del token
     */
    @PostMapping("/{id}/anular/{usuarioId:.*}")  // ⬅️ El ".*" hace que acepte cualquier cosa
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> anular(
        @PathVariable Long id,
        @PathVariable(required = false) String usuarioId,  // ⬅️ String para aceptar "undefined"
        @Valid @RequestBody AnularFacturaRequest request,
        Authentication authentication) {

        log.info("POST /api/facturas-internas/{}/anular - usuarioId recibido: '{}'", id, usuarioId);

        try {
            Long usuarioIdFinal = null;

            // Intentar parsear el usuarioId si viene y no es "undefined"
            if (usuarioId != null && !usuarioId.isEmpty() && !"undefined".equals(usuarioId)) {
                try {
                    usuarioIdFinal = Long.parseLong(usuarioId);
                    log.info("✅ Usuario obtenido del path: {}", usuarioIdFinal);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ usuarioId inválido en path: '{}', usando token", usuarioId);
                }
            }

            // Si no viene o es inválido, obtener del token
            if (usuarioIdFinal == null) {
                ContextoUsuario contexto = (ContextoUsuario) authentication.getPrincipal();
                usuarioIdFinal = contexto.getUserId();
                log.info("✅ Usuario obtenido del token: {}", usuarioIdFinal);
            }

            facturaInternaService.anular(id, usuarioIdFinal, request);
            return ResponseEntity.ok(ApiResponse.success("Factura anulada exitosamente", null));

        } catch (Exception e) {
            log.error("❌ Error al anular factura: ", e);
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

    /**
     * Listar facturas con filtros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Page<FacturaInternaListResponse>>> listar(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String fechaDesde,  // ⬅️ NUEVO
        @RequestParam(required = false) String fechaHasta,  // ⬅️ NUEVO
        @RequestParam(required = false) String busqueda,    // ⬅️ NUEVO
        Pageable pageable) {
        log.info("GET /api/facturas-internas - Listando facturas con filtros");

        try {
            Page<FacturaInternaListResponse> facturas = facturaInternaService.buscar(
                empresaId, sucursalId, estado, fechaDesde, fechaHasta, busqueda, pageable);
            return ResponseEntity.ok(ApiResponse.success("Facturas encontradas", facturas));
        } catch (Exception e) {
            log.error("Error al listar facturas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener facturas"));
        }
    }

    /**
     * Cambiar métodos de pago de una factura
     */
    @PutMapping("/{id}/metodos-pago")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> cambiarMetodosPago(
        @PathVariable Long id,
        @PathVariable(required = false) String usuarioId,  // Para mantener compatibilidad
        @Valid @RequestBody CambiarMetodosPagoRequest request,
        Authentication authentication) {

        log.info("PUT /api/facturas-internas/{}/metodos-pago", id);

        try {
            // Obtener usuarioId del token (misma lógica que anular)
            Long usuarioIdFinal = null;

            if (usuarioId != null && !usuarioId.isEmpty() && !"undefined".equals(usuarioId)) {
                try {
                    usuarioIdFinal = Long.parseLong(usuarioId);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ usuarioId inválido, usando token");
                }
            }

            if (usuarioIdFinal == null) {
                ContextoUsuario contexto = (ContextoUsuario) authentication.getPrincipal();
                usuarioIdFinal = contexto.getUserId();
            }

            facturaInternaService.cambiarMetodosPago(id, usuarioIdFinal, request);
            return ResponseEntity.ok(ApiResponse.success("Métodos de pago actualizados exitosamente", null));

        } catch (BadRequestException e) {
            log.error("Error de validación: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al cambiar métodos de pago: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al actualizar métodos de pago: " + e.getMessage()));
        }
    }
}