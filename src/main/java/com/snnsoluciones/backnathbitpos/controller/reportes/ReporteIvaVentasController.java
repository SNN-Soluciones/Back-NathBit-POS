package com.snnsoluciones.backnathbitpos.controller.reportes;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasResponse;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteIvaVentasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller para el reporte de IVA por tarifa en ventas.
 *
 * <p>Expone dos endpoints:</p>
 * <ul>
 *   <li><b>POST /generar</b> — cuerpo JSON con todos los filtros (recomendado para el frontend).</li>
 *   <li><b>GET  /generar</b> — parámetros de URL para integraciones simples o pruebas rápidas.</li>
 * </ul>
 *
 * <h3>Todos los filtros son opcionales excepto {@code sucursalId}.</h3>
 * <p>Los valores por defecto se aplican automáticamente en el servicio:</p>
 * <ul>
 *   <li>fechaEmisionDesde  → primer día del mes anterior</li>
 *   <li>fechaEmisionHasta  → último día del mes anterior</li>
 *   <li>tiposDocumento     → FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_CREDITO</li>
 *   <li>estadoBitacora     → ACEPTADA</li>
 *   <li>fechaAceptacionDesde → 2026-02-01</li>
 *   <li>fechaAceptacionHasta → hoy</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes/iva-ventas")
@RequiredArgsConstructor
@Tag(
    name  = "Reporte IVA Ventas",
    description = "Reporte de IVA por tarifa (0%, 1%, 2%, 4%, 8%, 13%) en documentos de venta"
)
public class ReporteIvaVentasController {

    private final ReporteIvaVentasService reporteIvaVentasService;

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/reportes/iva-ventas/generar
    //  Recomendado: el frontend envía el body JSON con los filtros que necesite
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Generar reporte IVA ventas (POST)",
        description = """
            Genera el desglose de IVA por tarifa para facturas, tiquetes y notas de crédito.
            
            **Único campo obligatorio:** `sucursalId`.
            
            Todos los demás filtros son opcionales; si se omiten se aplican los siguientes defaults:
            
            | Filtro               | Default                              |
            |----------------------|--------------------------------------|
            | fechaEmisionDesde    | primer día del mes anterior          |
            | fechaEmisionHasta    | último día del mes anterior          |
            | tiposDocumento       | [FE, TE, NC]                         |
            | estadoBitacora       | ACEPTADA                             |
            | fechaAceptacionDesde | 2026-02-01                           |
            | fechaAceptacionHasta | hoy                                  |
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Filtros del reporte. Solo sucursalId es obligatorio.",
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name  = "Mínimo (solo sucursal)",
                    value = """
                        {
                          "sucursalId": 2
                        }
                        """
                ),
                @ExampleObject(
                    name  = "Con todos los filtros",
                    value = """
                        {
                          "sucursalId": 2,
                          "fechaEmisionDesde": "2026-01-01",
                          "fechaEmisionHasta": "2026-02-28",
                          "tiposDocumento": ["FACTURA_ELECTRONICA", "NOTA_CREDITO"],
                          "estadoBitacora": "ACEPTADA",
                          "fechaAceptacionDesde": "2026-02-01",
                          "fechaAceptacionHasta": "2026-03-22"
                        }
                        """
                ),
                @ExampleObject(
                    name  = "Solo facturas rechazadas",
                    value = """
                        {
                          "sucursalId": 2,
                          "tiposDocumento": ["FACTURA_ELECTRONICA"],
                          "estadoBitacora": "RECHAZADA"
                        }
                        """
                )
            }
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Reporte generado exitosamente"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description  = "sucursalId faltante o parámetros inválidos"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description  = "Sucursal no encontrada"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description  = "Error interno del servidor"
        )
    })
    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<ReporteIvaVentasResponse>> generarReportePost(
        @Valid @RequestBody ReporteIvaVentasRequest request
    ) {
        log.info("POST /api/reportes/iva-ventas/generar — sucursal={}", request.getSucursalId());

        try {
            ReporteIvaVentasResponse response = reporteIvaVentasService.generarReporte(request);

            String mensaje = String.format(
                "Reporte IVA generado: %d documentos encontrados",
                response.getTotalDocumentos()
            );

            return ResponseEntity.ok(ApiResponse.ok(mensaje, response));

        } catch (jakarta.persistence.EntityNotFoundException ex) {
            log.warn("Sucursal no encontrada: {}", ex.getMessage());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));

        } catch (Exception ex) {
            log.error("Error generando reporte IVA", ex);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al generar el reporte: " + ex.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/reportes/iva-ventas/generar
    //  Alternativa ligera para integraciones simples / pruebas en browser
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Generar reporte IVA ventas (GET)",
        description = """
            Versión GET del mismo reporte. Útil para pruebas rápidas o integraciones
            que prefieren query params. Comportamiento idéntico al POST.
            
            Ejemplo mínimo:
            ```
            GET /api/reportes/iva-ventas/generar?sucursalId=2
            ```
            
            Ejemplo con todos los filtros:
            ```
            GET /api/reportes/iva-ventas/generar
                ?sucursalId=2
                &fechaEmisionDesde=2026-01-01
                &fechaEmisionHasta=2026-02-28
                &tiposDocumento=FACTURA_ELECTRONICA&tiposDocumento=NOTA_CREDITO
                &estadoBitacora=ACEPTADA
                &fechaAceptacionDesde=2026-02-01
                &fechaAceptacionHasta=2026-03-22
            ```
            """
    )
    @GetMapping("/generar")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<ReporteIvaVentasResponse>> generarReporteGet(

        @Parameter(description = "ID de la sucursal (obligatorio)", example = "2", required = true)
        @RequestParam Long sucursalId,

        @Parameter(description = "Fecha emisión desde (yyyy-MM-dd). Default: primer día del mes anterior")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaEmisionDesde,

        @Parameter(description = "Fecha emisión hasta (yyyy-MM-dd). Default: último día del mes anterior")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaEmisionHasta,

        @Parameter(description = "Tipos de documento. Default: FACTURA_ELECTRONICA, TIQUETE_ELECTRONICO, NOTA_CREDITO")
        @RequestParam(required = false)
        List<String> tiposDocumento,

        @Parameter(description = "Estado en bitácora de Hacienda. Default: ACEPTADA")
        @RequestParam(required = false, defaultValue = "ACEPTADA")
        String estadoBitacora,

        @Parameter(description = "Fecha aceptación desde (yyyy-MM-dd). Default: 2026-02-01")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaAceptacionDesde,

        @Parameter(description = "Fecha aceptación hasta (yyyy-MM-dd). Default: hoy")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaAceptacionHasta

    ) {
        log.info("GET /api/reportes/iva-ventas/generar — sucursal={}", sucursalId);

        // Armar el mismo request que usa el POST
        ReporteIvaVentasRequest request = ReporteIvaVentasRequest.builder()
            .sucursalId(sucursalId)
            .fechaEmisionDesde(fechaEmisionDesde)
            .fechaEmisionHasta(fechaEmisionHasta)
            .tiposDocumento(tiposDocumento)
            .estadoBitacora(estadoBitacora)
            .fechaAceptacionDesde(fechaAceptacionDesde)
            .fechaAceptacionHasta(fechaAceptacionHasta)
            .build();

        // Reutilizar la misma lógica del POST
        return generarReportePost(request);
    }
}