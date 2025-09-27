package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.repository.FacturaInternaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

/**
 * Servicio para generar PDFs de comandas de cocina
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComandaCocinaService {

    private final ComandaCocinaMapperService mapperService;
    private final PdfGeneratorService pdfGeneratorService;
    private final FacturaInternaRepository facturaInternaRepository;

    /**
     * Genera PDF de comanda de cocina en formato 80mm
     *
     * @param numeroInterno Número interno de la factura (ej: INT-2024-00001)
     * @return PDF en bytes
     */
    @Transactional(readOnly = true)
    public byte[] generarComandaCocina(String numeroInterno) {
        log.info("Generando comanda de cocina para: {}", numeroInterno);

        try {
            // 1. Mapear datos de la factura interna
            Map<String, Object> parametros = mapperService.mapearComandaCocina(numeroInterno);

            // 2. Generar PDF con plantilla de comanda
            String templatePath = "comanda_cocina";
            
            // El mapper ya incluye el datasource en los parámetros
            return pdfGeneratorService.generarReporte(
                templatePath,
                parametros,
                (JRBeanCollectionDataSource) parametros.get("datasource_detalles")
            );

        } catch (Exception e) {
            log.error("Error generando comanda de cocina para {}: {}", numeroInterno, e.getMessage(), e);
            throw new RuntimeException("Error al generar comanda de cocina: " + e.getMessage(), e);
        }
    }

    /**
     * Genera comanda de cocina por ID de factura interna
     * 
     * @param facturaId ID de la factura interna
     * @return PDF en bytes
     */
    @Transactional(readOnly = true)
    public byte[] generarComandaCocinaByFacturaId(Long facturaId) {
        String numeroInterno = facturaInternaRepository.findById(facturaId)
            .map(factura -> factura.getNumero())
            .orElseThrow(() -> new RuntimeException("Factura interna no encontrada con ID: " + facturaId));

        return generarComandaCocina(numeroInterno);
    }
}