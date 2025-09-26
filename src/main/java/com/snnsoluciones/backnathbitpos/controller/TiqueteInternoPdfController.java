package com.snnsoluciones.backnathbitpos.controller;
import com.snnsoluciones.backnathbitpos.service.pdf.TiqueteInternoPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/tiquetes-internos")
@RequiredArgsConstructor
@Tag(name = "Tiquetes Internos", description = "Endpoints para generar PDFs de tiquetes internos")
public class TiqueteInternoPdfController {

    private final TiqueteInternoPdfService tiqueteInternoPdfService;

    @Operation(summary = "Generar PDF de tiquete interno",
        description = "Genera un PDF de tiquete interno en formato 80mm para impresión térmica")
    @GetMapping("/pdf/{numeroInterno}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> generarPdfTiqueteInterno(
        @Parameter(description = "Número interno del tiquete (ej: INT-2024-00001)", required = true)
        @PathVariable String numeroInterno,
        
        @Parameter(description = "Si es true, descarga el archivo. Si es false, lo muestra en el navegador")
        @RequestParam(defaultValue = "false") boolean descargar) {

        log.info("Generando PDF para tiquete interno: {}", numeroInterno);

        try {
            // Generar PDF
            byte[] pdfBytes = tiqueteInternoPdfService.generarTiqueteInterno(numeroInterno);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            // Nombre del archivo
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("tiquete_%s_%s.pdf", numeroInterno.replace("/", "-"), timestamp);

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
            log.error("Error generando PDF para tiquete {}: {}", numeroInterno, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }

    @Operation(summary = "Vista previa de tiquete interno",
        description = "Genera una vista previa del PDF del tiquete en el navegador")
    @GetMapping("/preview/{numeroInterno}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> previewTiqueteInterno(
        @PathVariable String numeroInterno) {

        return generarPdfTiqueteInterno(numeroInterno, false);
    }

    @Operation(summary = "Descargar PDF de tiquete interno",
        description = "Descarga el PDF del tiquete interno")
    @GetMapping("/download/{numeroInterno}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> downloadTiqueteInterno(
        @PathVariable String numeroInterno) {

        return generarPdfTiqueteInterno(numeroInterno, true);
    }

    @Operation(summary = "Reimprimir tiquete por ID de factura",
        description = "Reimprime un tiquete interno usando el ID de la factura")
    @GetMapping("/reimprimir/{facturaId}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> reimprimirPorFacturaId(
        @Parameter(description = "ID de la factura interna", required = true)
        @PathVariable Long facturaId,
        
        @RequestParam(defaultValue = "false") boolean descargar) {

        log.info("Reimprimiendo tiquete para factura ID: {}", facturaId);

        try {
            // Obtener número interno por ID
            String numeroInterno = tiqueteInternoPdfService.obtenerNumeroInternoPorFacturaId(facturaId);
            
            // Delegar al método principal
            return generarPdfTiqueteInterno(numeroInterno, descargar);

        } catch (Exception e) {
            log.error("Error al reimprimir tiquete para factura {}: {}", facturaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
}