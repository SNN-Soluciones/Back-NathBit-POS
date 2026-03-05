package com.snnsoluciones.backnathbitpos.service.pdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReciboPagoPdfService {

    private final ReciboPagoPdfMapperService mapperService;
    private final PdfGeneratorService pdfGeneratorService;

    @Transactional(readOnly = true)
    public byte[] generarRecibo(Long pagoId) {
        log.info("Generando recibo de pago para ID: {}", pagoId);

        try {
            Map<String, Object> parametros = mapperService.mapearPagoAParametros(pagoId);

            return pdfGeneratorService.generarPdf(
                "recibo_pago_80mm",
                parametros,
                new ArrayList<>() // Sin subreporte, todo en parámetros
            );

        } catch (Exception e) {
            log.error("Error generando recibo para pago {}: {}", pagoId, e.getMessage(), e);
            throw new RuntimeException("Error al generar recibo: " + e.getMessage(), e);
        }
    }
}