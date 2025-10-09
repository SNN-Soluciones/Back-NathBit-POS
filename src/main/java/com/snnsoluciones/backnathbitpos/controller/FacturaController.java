package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import com.snnsoluciones.backnathbitpos.service.impl.FacturaResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
        @RequestBody CrearFacturaRequest request) {

        log.info("Creando factura tipo: {} para cliente: {}",
            request.getTipoDocumento(), request.getClienteId());

        try {
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

    @Operation(summary = "Buscar facturas para referencias",
        description = "Busca facturas por clave, consecutivo, nombre de cliente o fechas para ser usadas como referencia")
    @PostMapping("/buscar-para-referencia/{empresaId}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<FacturaReferenciaDto>>> buscarParaReferencia(
        @Valid @RequestBody BuscarFacturaReferenciaRequest request, @PathVariable Long empresaId) {

        try {
            // Obtener empresa del contexto de usuario
            request.setEmpresaId(empresaId);

            Page<FacturaReferenciaDto> facturas = facturaService.buscarParaReferencia(request);

            return ResponseEntity.ok(ApiResponse.ok(
                "Facturas encontradas: " + facturas.getTotalElements(),
                facturas
            ));

        } catch (Exception e) {
            log.error("Error al buscar facturas para referencia: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al buscar facturas: " + e.getMessage()));
        }
    }

    /**
     * Genera un reporte Excel de ventas para Hacienda en un rango de fechas
     *
     * GET /api/facturas/reporte-hacienda?empresaId=1&sucursalId=1&fechaInicio=2025-01-01&fechaFin=2025-01-31
     *
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal
     * @param fechaInicio Fecha de inicio del rango (formato: yyyy-MM-dd)
     * @param fechaFin Fecha de fin del rango (formato: yyyy-MM-dd)
     * @return Archivo Excel descargable
     */
    @GetMapping("/reporte-hacienda")
    public ResponseEntity<byte[]> generarReporteHacienda(
        @RequestParam Long empresaId,
        @RequestParam Long sucursalId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        log.info("🚀 Generando reporte de ventas para Hacienda - Empresa: {}, Sucursal: {}, Rango: {} a {}",
            empresaId, sucursalId, fechaInicio, fechaFin);

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
            byte[] excelBytes = facturaService.generarReporteHacienda(empresaId, sucursalId, fechaInicio, fechaFin);

            // Nombre del archivo
            String filename = String.format("Ventas_Hacienda_%s_%s.xlsx",
                fechaInicio.format(DateTimeFormatter.BASIC_ISO_DATE),
                fechaFin.format(DateTimeFormatter.BASIC_ISO_DATE));

            // Headers para descarga
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("✅ Reporte Excel generado exitosamente: {} bytes", excelBytes.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);

        } catch (Exception e) {
            log.error("❌ Error generando reporte Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}