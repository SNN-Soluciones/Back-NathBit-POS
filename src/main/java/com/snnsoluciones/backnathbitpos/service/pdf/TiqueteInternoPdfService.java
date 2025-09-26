package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.repository.FacturaInternaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

/**
 * Servicio para generar PDFs de tiquetes internos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TiqueteInternoPdfService {

    private final TiqueteInternoPdfMapperService mapperService;
    private final PdfGeneratorService pdfGeneratorService;
    private final FacturaInternaRepository facturaInternaRepository;

    /**
     * Genera PDF de tiquete interno en formato 80mm
     *
     * @param numeroInterno Número interno del tiquete (ej: INT-2024-00001)
     * @return PDF en bytes
     */
    @Transactional(readOnly = true)
    public byte[] generarTiqueteInterno(String numeroInterno) {
        log.info("Generando PDF para tiquete interno: {}", numeroInterno);

        try {
            // 1. Mapear datos del tiquete
            Map<String, Object> parametros = mapperService.mapearTiqueteInternoAParametros(numeroInterno);

            // 2. Generar PDF con plantilla de tiquete interno
            String templatePath = "tiquete_interno_80mm";

            // El mapper ya incluye el datasource en los parámetros
            // Usamos generarPdf que espera una lista de datos (en este caso vacía porque el datasource está en los parámetros)
            return pdfGeneratorService.generarPdf(
                templatePath,
                parametros,
                new ArrayList<>() // Lista vacía porque el datasource ya está en los parámetros
            );

        } catch (Exception e) {
            log.error("Error generando PDF para tiquete {}: {}", numeroInterno, e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF del tiquete: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el número interno de una factura por su ID
     *
     * @param facturaId ID de la factura
     * @return Número interno
     */
    @Transactional(readOnly = true)
    public String obtenerNumeroInternoPorFacturaId(Long facturaId) {
        return facturaInternaRepository.findById(facturaId)
            .map(facturaInterna -> facturaInterna.getNumero())
            .orElseThrow(() -> new RuntimeException("Factura interna no encontrada con ID: " + facturaId));
    }
}