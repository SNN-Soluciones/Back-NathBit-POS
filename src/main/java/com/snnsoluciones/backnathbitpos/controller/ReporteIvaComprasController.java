package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasRequest;
import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasResponse;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteIvaComprasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reportes/iva-compras")
@RequiredArgsConstructor
@Tag(name = "Reporte IVA Compras",
     description = "Reporte de IVA por tarifa (0%, 1%, 2%, 4%, 8%, 13%) en documentos de compra recibidos")
public class ReporteIvaComprasController {

    private final ReporteIvaComprasService reporteIvaComprasService;

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/reportes/iva-compras/generar — JSON
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Generar reporte IVA compras (JSON)",
        description = "Genera el desglose de IVA por tarifa para facturas, tiquetes y notas de crédito recibidas. "
                    + "Único campo obligatorio: sucursalId."
    )
    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<ReporteIvaComprasResponse>> generarReporte(
        @Valid @RequestBody ReporteIvaComprasRequest request
    ) {
        log.info("POST /api/reportes/iva-compras/generar — sucursal={}", request.getSucursalId());

        try {
            ReporteIvaComprasResponse response = reporteIvaComprasService.generarReporte(request);

            String msg = String.format("Reporte IVA Compras generado: %d documentos encontrados",
                response.getTotalDocumentos());

            return ResponseEntity.ok(ApiResponse.ok(msg, response));

        } catch (jakarta.persistence.EntityNotFoundException ex) {
            log.warn("Sucursal no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));

        } catch (Exception ex) {
            log.error("Error generando reporte IVA Compras", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al generar el reporte: " + ex.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST /api/reportes/iva-compras/exportar-excel — blob xlsx
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Exportar reporte IVA compras a Excel",
        description = "Genera y descarga un archivo .xlsx con el desglose de IVA por tarifa en compras. "
                    + "Mismo body JSON que /generar."
    )
    @PostMapping(
        value    = "/exportar-excel",
        produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> exportarExcel(
        @Valid @RequestBody ReporteIvaComprasRequest request
    ) {
        log.info("POST /api/reportes/iva-compras/exportar-excel — sucursal={}", request.getSucursalId());

        try {
            // Aplicar defaults antes de construir el nombre del archivo
            request.aplicarDefaults();

            byte[] excelBytes = reporteIvaComprasService.generarExcel(request);

            String filename = String.format("IVA_Compras_%d_%s_%s.xlsx",
                request.getSucursalId(),
                request.getFechaEmisionDesde(),
                request.getFechaEmisionHasta());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok().headers(headers).body(excelBytes);

        } catch (jakarta.persistence.EntityNotFoundException ex) {
            log.warn("Sucursal no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception ex) {
            log.error("Error exportando Excel IVA Compras", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}