package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.reporte.*;
import com.snnsoluciones.backnathbitpos.service.reportes.ReporteVentasTipoPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para reportes de ventas por tipo de pago Siguiendo el principio KISS - Keep It Simple,
 * Stupid!
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes/ventas-tipo-pago")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes de ventas por tipo de pago")
public class ReporteVentasTipoPagoController {

  private final ReporteVentasTipoPagoService service;

  @Operation(summary = "Generar reporte de ventas por tipo de pago",
      description = "Genera reporte agrupado por medios de pago (Efectivo, Tarjeta, etc)")
  @PostMapping("/generar")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<?> generarReporte(
      @Valid @RequestBody ReporteVentasTipoPagoRequest request) {

    try {
      log.info("Generando reporte de ventas por tipo de pago. Sucursal: {}, Periodo: {} - {}",
          request.getSucursalId(), request.getFechaDesde(), request.getFechaHasta());

      ReporteVentasTipoPagoResponse response = service.generarReporte(request);

      // Si es descarga de archivo
      if (response.getArchivoGenerado() != null) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + response.getNombreArchivo() + "\"")
            .contentType(MediaType.parseMediaType(response.getTipoContenido()))
            .body(response.getArchivoGenerado());
      }

      // Si es JSON
      return ResponseEntity.ok(ApiResponse.ok(
          String.format("Reporte generado: %d tipos de pago, Total: %s",
              response.getDetalles().size(),
              response.getTotalGeneral()),
          response
      ));

    } catch (JRException e) {
      log.error("Error generando reporte Jasper", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error generando reporte: " + e.getMessage()));
    } catch (Exception e) {
      log.error("Error inesperado", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @Operation(summary = "Preview de datos del reporte",
      description = "Obtiene los datos sin generar el archivo")
  @PostMapping("/preview")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
  public ResponseEntity<ApiResponse<ReporteVentasTipoPagoResponse>> previewReporte(
      @Valid @RequestBody ReporteVentasTipoPagoRequest request) {

    try {
      // Forzar formato JSON para preview
      request.setFormato(FormatoReporte.JSON);

      ReporteVentasTipoPagoResponse datos = service.obtenerDatosReporte(request);

      if (datos.getDetalles().isEmpty()) {
        return ResponseEntity.ok(ApiResponse.ok(
            "No se encontraron ventas en el período especificado",
            datos
        ));
      }

      return ResponseEntity.ok(ApiResponse.ok(
          String.format("Se encontraron %d tipos de pago diferentes",
              datos.getDetalles().size()),
          datos
      ));

    } catch (Exception e) {
      log.error("Error obteniendo preview", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }
}