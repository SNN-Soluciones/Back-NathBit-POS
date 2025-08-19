package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.impl.FacturaResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Gestión de facturas y documentos electrónicos")
public class FacturaController {

    private final FacturaService facturaService;
    private final FacturaResponseBuilder responseBuilder;

    @Operation(summary = "Crear nueva factura",
        description = "Crea una factura con soporte para otros cargos, descuentos y múltiples monedas")
    @PostMapping
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> crear(
        @Valid @RequestBody CrearFacturaRequest request) {

        log.info("Creando factura tipo: {} para cliente: {}",
            request.getTipoDocumento(), request.getClienteId());

        try {
            // Validación adicional del request
            if (!request.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Datos de factura inválidos"));
            }

            // Crear factura
            Factura factura = facturaService.crear(request);

            // Construir response
            FacturaResponse response = responseBuilder.construirResponse(factura);

            log.info("Factura creada exitosamente: {}", factura.getConsecutivo());

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                    "Factura creada exitosamente",
                    response
                ));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al crear factura: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error inesperado al crear factura", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al crear la factura: " + e.getMessage()));
        }
    }

    @Operation(summary = "Buscar factura por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> buscarPorId(@PathVariable Long id) {
        return facturaService.buscarPorId(id)
            .map(factura -> ResponseEntity.ok(
                ApiResponse.ok(responseBuilder.construirResponse(factura))
            ))
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar factura por clave")
    @GetMapping("/clave/{clave}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> buscarPorClave(@PathVariable String clave) {
        return facturaService.buscarPorClave(clave)
            .map(factura -> ResponseEntity.ok(
                ApiResponse.ok(responseBuilder.construirResponse(factura))
            ))
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar factura por consecutivo")
    @GetMapping("/consecutivo/{consecutivo}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> buscarPorConsecutivo(
        @PathVariable String consecutivo) {
        return facturaService.buscarPorConsecutivo(consecutivo)
            .map(factura -> ResponseEntity.ok(
                ApiResponse.ok(responseBuilder.construirResponse(factura))
            ))
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Listar facturas de sesión actual")
    @GetMapping("/sesion-actual")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<List<FacturaListaResponse>>> listarSesionActual(
        @RequestParam Long sesionCajaId) {

        List<Factura> facturas = facturaService.listarPorSesionCaja(sesionCajaId);
        List<FacturaListaResponse> response = facturas.stream()
            .map(responseBuilder::construirListaResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(
            String.format("Se encontraron %d facturas", response.size()),
            response
        ));
    }

    @Operation(summary = "Listar facturas con error")
    @GetMapping("/errores/{sucursalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<List<FacturaListaResponse>>> listarConError(
        @PathVariable Long sucursalId) {

        List<Factura> facturas = facturaService.listarFacturasConError(sucursalId);
        List<FacturaListaResponse> response = facturas.stream()
            .map(responseBuilder::construirListaResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(
            String.format("Se encontraron %d facturas con error", response.size()),
            response
        ));
    }

    @Operation(summary = "Anular factura",
        description = "Anula una factura. En el futuro generará nota de crédito si es necesario")
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> anular(
        @PathVariable Long id,
        @RequestBody @Valid AnularFacturaRequest request) {

        try {
            Factura factura = facturaService.anular(id, request.getMotivo());
            FacturaResponse response = responseBuilder.construirResponse(factura);

            return ResponseEntity.ok(ApiResponse.ok(
                "Factura anulada exitosamente",
                response
            ));

        } catch (RuntimeException e) {
            log.error("Error al anular factura {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reenviar factura a Hacienda",
        description = "Reintenta el envío de una factura con error")
    @PostMapping("/{id}/reenviar")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> reenviar(@PathVariable Long id) {

        try {
            facturaService.reenviar(id);

            return ResponseEntity.ok(ApiResponse.ok(
                "Factura marcada para reenvío. El proceso se ejecutará en segundo plano."
            ));

        } catch (RuntimeException e) {
            log.error("Error al reenviar factura {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Validar totales de factura",
        description = "Endpoint de utilidad para validar cálculos antes de crear la factura")
    @PostMapping("/validar-totales")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<ValidacionTotalesResponse>> validarTotales(
        @Valid @RequestBody ValidacionTotalesRequest request) {

        try {
            ValidacionTotalesResponse response = facturaService.validarTotales(request);

            if (response.isEsValido()) {
                return ResponseEntity.ok(ApiResponse.ok(
                    "Validación exitosa",
                    response
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMensaje(), response));
            }

        } catch (Exception e) {
            log.error("Error al validar totales", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al validar totales: " + e.getMessage()));
        }
    }
}