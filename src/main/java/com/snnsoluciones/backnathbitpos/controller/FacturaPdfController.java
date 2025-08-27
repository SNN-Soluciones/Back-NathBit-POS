package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador para generar PDFs de facturas
 * Los PDFs son públicos (cualquiera con la clave puede acceder)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/facturas/pdf")
@RequiredArgsConstructor
@Tag(name = "Facturas PDF", description = "Generación de PDFs de facturas")
public class FacturaPdfController {

    private final FacturaPdfService facturaPdfService;

    @Operation(summary = "Generar PDF de factura por clave",
        description = "Genera un PDF de la factura en formato carta (A4) o ticket (80mm)")
    @GetMapping("/{clave}")
    public ResponseEntity<byte[]> generarPdf(
        @Parameter(description = "Clave numérica de la factura", required = true)
        @PathVariable String clave,
        @Parameter(description = "Formato del PDF: 'carta' (default) o 'ticket'")
        @RequestParam(defaultValue = "carta") String formato,
        @Parameter(description = "Si es true, descarga el archivo. Si es false, lo muestra en el navegador")
        @RequestParam(defaultValue = "false") boolean descargar) {

        log.info("Generando PDF para factura {} en formato {}", clave, formato);

        try {
            // Generar PDF
            byte[] pdfBytes = facturaPdfService.generarFactura(clave, formato);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            // Nombre del archivo
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("factura_%s_%s.pdf", clave, timestamp);

            if (descargar) {
                // Forzar descarga
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            } else {
                // Mostrar en navegador
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
            }

            // Cache headers para evitar regeneración innecesaria
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600"); // Cache por 1 hora
            headers.add(HttpHeaders.PRAGMA, "cache");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generando PDF para clave {}: {}", clave, e.getMessage());
            return ResponseEntity.internalServerError()
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }

    @Operation(summary = "Generar PDF ticket para punto de venta",
        description = "Endpoint específico para generar tickets de 80mm desde el punto de venta")
    @GetMapping("/ticket/{clave}")
    public ResponseEntity<byte[]> generarTicket(
        @Parameter(description = "Clave numérica de la factura", required = true)
        @PathVariable String clave) {

        return generarPdf(clave, "ticket", false);
    }

    @Operation(summary = "Vista previa de factura",
        description = "Genera una vista previa del PDF en el navegador")
    @GetMapping("/preview/{clave}")
    public ResponseEntity<byte[]> previewPdf(
        @PathVariable String clave,
        @RequestParam(defaultValue = "carta") String formato) {

        return generarPdf(clave, formato, false);
    }

    @Operation(summary = "Descargar PDF de factura",
        description = "Descarga el PDF de la factura")
    @GetMapping("/download/{clave}")
    public ResponseEntity<byte[]> downloadPdf(
        @PathVariable String clave,
        @RequestParam(defaultValue = "carta") String formato) {

        return generarPdf(clave, formato, true);
    }
}