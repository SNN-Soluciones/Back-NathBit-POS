package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.service.pdf.ComandaCocinaService;
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
@RequestMapping("/api/comandas-cocina")
@RequiredArgsConstructor
@Tag(name = "Comandas Cocina", description = "Endpoints para generar comandas de cocina")
public class ComandaCocinaController {

    private final ComandaCocinaService comandaCocinaService;

    @Operation(summary = "Generar comanda de cocina",
        description = "Genera una comanda de cocina en formato 80mm para impresión térmica")
    @GetMapping("/pdf/{numeroInterno}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'COCINA', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> generarComandaCocina(
        @Parameter(description = "Número interno de la factura (ej: INT-2024-00001)", required = true)
        @PathVariable String numeroInterno,
        
        @Parameter(description = "Si es true, descarga el archivo. Si es false, lo muestra en el navegador")
        @RequestParam(defaultValue = "false") boolean descargar) {

        log.info("Generando comanda de cocina para: {}", numeroInterno);

        try {
            // Generar PDF
            byte[] pdfBytes = comandaCocinaService.generarComandaCocina(numeroInterno);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            // Nombre del archivo
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String filename = String.format("comanda_%s_%s.pdf", 
                numeroInterno.replace("/", "-"), timestamp);

            if (descargar) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + filename + "\"");
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + filename + "\"");
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generando comanda para {}: {}", numeroInterno, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }

    @Operation(summary = "Vista previa de comanda",
        description = "Genera una vista previa de la comanda en el navegador")
    @GetMapping("/preview/{numeroInterno}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'COCINA', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> previewComanda(
        @PathVariable String numeroInterno) {

        return generarComandaCocina(numeroInterno, false);
    }

    @Operation(summary = "Generar comanda por ID de factura",
        description = "Genera una comanda usando el ID de la factura interna")
    @GetMapping("/factura/{facturaId}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'COCINA', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> generarComandaPorFacturaId(
        @Parameter(description = "ID de la factura interna", required = true)
        @PathVariable Long facturaId,
        
        @RequestParam(defaultValue = "false") boolean descargar) {

        log.info("Generando comanda para factura ID: {}", facturaId);

        try {
            byte[] pdfBytes = comandaCocinaService.generarComandaCocinaByFacturaId(facturaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String filename = String.format("comanda_factura_%d_%s.pdf", facturaId, timestamp);

            if (descargar) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + filename + "\"");
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + filename + "\"");
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generando comanda para factura {}: {}", facturaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("X-Error-Message", e.getMessage())
                .build();
        }
    }
}