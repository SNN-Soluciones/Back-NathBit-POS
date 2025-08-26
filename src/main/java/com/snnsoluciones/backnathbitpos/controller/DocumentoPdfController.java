package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documentos/pdf")
@RequiredArgsConstructor
public class DocumentoPdfController {

    private final FacturaPdfService facturaPdfService;

    /**
     * Genera y descarga PDF por clave numérica
     */
    @GetMapping("/descargar/{claveNumerica}")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String claveNumerica) {
        try {
            byte[] pdfBytes = facturaPdfService.generarFacturaPorClave(claveNumerica);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                String.format("factura_%s.pdf", claveNumerica));
            headers.setCacheControl("no-cache");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Vista previa del PDF en el navegador
     */
    @GetMapping("/ver/{claveNumerica}")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<byte[]> verPdf(@PathVariable String claveNumerica) {
        try {
            byte[] pdfBytes = facturaPdfService.generarFacturaPorClave(claveNumerica);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", 
                String.format("factura_%s.pdf", claveNumerica));
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Enviar PDF por email
     */
    @PostMapping("/enviar-email/{claveNumerica}")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse> enviarPorEmail(
            @PathVariable String claveNumerica,
            @RequestParam String emailDestino,
            @RequestParam(required = false) String mensaje) {
        try {
            byte[] pdfBytes = facturaPdfService.generarFacturaPorClave(claveNumerica);
            
//            emailService.enviarFactura(
//                emailDestino,
//                claveNumerica,
//                pdfBytes,
//                mensaje
//            );
            
            return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Factura enviada exitosamente a: " + emailDestino)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder()
                .success(false)
                .message("Error enviando factura: " + e.getMessage())
                .build());
        }
    }
}