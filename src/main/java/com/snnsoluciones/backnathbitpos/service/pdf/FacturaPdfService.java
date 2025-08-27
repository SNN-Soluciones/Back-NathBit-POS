package com.snnsoluciones.backnathbitpos.service.pdf;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Servicio para generar PDFs de facturas
 * Conecta con datos reales de la BD
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    private final PdfGeneratorService pdfGeneratorService;
    private final FacturaPdfMapperService mapperService;

    /**
     * Genera PDF de factura en formato carta (A4)
     * @param clave Clave numérica de la factura
     * @return PDF en bytes
     */
    @Transactional(readOnly = true)
    public byte[] generarFacturaCarta(String clave) {
        log.info("Generando PDF formato carta para factura: {}", clave);

        try {
            // 1. Mapear datos de la factura
            Map<String, Object> parametros = mapperService.mapearFacturaAParametros(clave, false);

            // 2. Generar PDF con plantilla carta
            String templatePath = "factura_electronica";
            return pdfGeneratorService.generarPdf(
                templatePath,
                parametros,
                (List<?>) parametros.get("detalles")
            );


        } catch (Exception e) {
            log.error("Error generando PDF carta para factura {}: {}", clave, e.getMessage());
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Genera PDF de factura en formato ticket (80mm)
     * @param clave Clave numérica de la factura
     * @return PDF en bytes
     */
    @Transactional(readOnly = true)
    public byte[] generarFacturaTicket(String clave) {
        log.info("Generando PDF formato ticket para factura: {}", clave);

        try {
            // 1. Mapear datos de la factura
            Map<String, Object> parametros = mapperService.mapearFacturaAParametros(clave, true);

            // 2. Generar PDF con plantilla ticket
            String templatePath = "factura_electronica_80mm";
            return pdfGeneratorService.generarPdf(templatePath, parametros,
                (List<?>) parametros.get("detalles")
            );

        } catch (Exception e) {
            log.error("Error generando PDF ticket para factura {}: {}", clave, e.getMessage());
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Genera PDF según el tipo especificado
     * @param clave Clave numérica de la factura
     * @param formato "carta" o "ticket"
     * @return PDF en bytes
     */
    public byte[] generarFactura(String clave, String formato) {
        if ("ticket".equalsIgnoreCase(formato) || "80mm".equalsIgnoreCase(formato)) {
            return generarFacturaTicket(clave);
        } else {
            return generarFacturaCarta(clave);
        }
    }
}