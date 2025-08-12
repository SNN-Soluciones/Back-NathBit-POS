package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaAsyncProcessor;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaAsyncProcessorImpl implements FacturaAsyncProcessor {

    private final FacturaJobService jobService;
    private final FacturaRepository facturaRepository;
    private final FacturaDocumentoRepository documentoRepository;
    private final StorageService storageService;

    // URLs de Hacienda
    @Value("${hacienda.api.url.sandbox:https://api-sandbox.comprobanteselectronicos.go.cr/recepcion/v1}")
    private String urlHaciendaSandbox;

    @Value("${hacienda.api.url.produccion:https://api.comprobanteselectronicos.go.cr/recepcion/v1}")
    private String urlHaciendaProduccion;

    // Configuración del procesador
    @Value("${factura.processor.batch-size:10}")
    private int batchSize;

    @Value("${factura.processor.enabled:true}")
    private boolean processorEnabled;

    @Override
    @Scheduled(fixedDelayString = "${factura.processor.delay:30000}") // 30 segundos
    public void procesarJobsPendientes() {
        if (!processorEnabled) {
            return;
        }

        log.debug("Iniciando procesamiento de jobs pendientes...");
        List<FacturaJob> jobs = jobService.obtenerJobsPendientes(batchSize);

        for (FacturaJob job : jobs) {
            try {
                procesarFactura(job);
            } catch (Exception e) {
                log.error("Error procesando job {}: {}", job.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional
    public void procesarFactura(FacturaJob job) {
        log.info("Procesando factura con clave: {}", job.getClave());

        try {
            Factura factura = facturaRepository.findById(job.getFacturaId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + job.getFacturaId()));

            // Actualizar estado de factura
            factura.setEstado(EstadoFactura.PROCESANDO);
            facturaRepository.save(factura);

            // Ejecutar pasos según el estado actual
            switch (job.getPasoActual()) {
                case GENERAR_XML -> {
                    String xmlPath = generarXML(factura.getId());
                    job.setPasoActual(PasoFacturacion.FIRMAR_DOCUMENTO);
                    jobService.marcarError(job.getId(), null); // Actualiza el job
                }

                case FIRMAR_DOCUMENTO -> {
                    String xmlUnsigned = obtenerArchivoPath(job.getClave(), TipoArchivoFactura.XML_UNSIGNED);
                    String xmlSigned = firmarXML(xmlUnsigned);
                    job.setPasoActual(PasoFacturacion.ENVIAR_HACIENDA);
                }

                case ENVIAR_HACIENDA -> {
                    String xmlSigned = obtenerArchivoPath(job.getClave(), TipoArchivoFactura.XML_SIGNED);
                    String respuesta = enviarHacienda(xmlSigned, job.getClave());
                    procesarRespuestaHacienda(factura, respuesta);
                    job.setPasoActual(PasoFacturacion.GENERAR_PDF);
                }

                case GENERAR_PDF -> {
                    String pdfPath = generarPDF(factura.getId());
                    job.setPasoActual(PasoFacturacion.ENVIAR_EMAIL);
                }

                case ENVIAR_EMAIL -> {
                    String pdfPath = obtenerArchivoPath(job.getClave(), TipoArchivoFactura.PDF_FACTURA);
                    String xmlPath = obtenerArchivoPath(job.getClave(), TipoArchivoFactura.XML_SIGNED);
                    enviarEmail(factura.getId(), pdfPath, xmlPath);
                    job.setPasoActual(PasoFacturacion.COMPLETADO);
                }

                case COMPLETADO -> {
                    jobService.marcarCompletado(job.getId());
                    return;
                }
            }

        } catch (Exception e) {
            log.error("Error en paso {} para factura {}: {}",
                job.getPasoActual(), job.getClave(), e.getMessage());
            jobService.marcarError(job.getId(), e.getMessage());
        }
    }

    @Override
    public String generarXML(Long facturaId) {
        log.info("Generando XML para factura: {}", facturaId);

        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        // TODO: Implementar generación real de XML según esquema de Hacienda
        // Por ahora, un XML de ejemplo
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FacturaElectronica xmlns="https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.3/facturaElectronica">
                <Clave>%s</Clave>
                <NumeroConsecutivo>%s</NumeroConsecutivo>
                <!-- TODO: Completar estructura XML -->
            </FacturaElectronica>
            """.formatted(factura.getClave(), factura.getConsecutivo());

        // Guardar en S3
        String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
        String key = FacturaDocumento.generarS3Key(empresaNombre, factura.getClave(), TipoArchivoFactura.XML_UNSIGNED);
        storageService.uploadFile(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
            key,
            "application/xml",
            xml.length()
        );

        // Registrar documento
        guardarDocumento(factura, TipoArchivoFactura.XML_UNSIGNED, key, xml.length());

        return key;
    }

    @Override
    public String firmarXML(String xmlPath) throws IOException {
        log.info("Firmando XML: {}", xmlPath);

        // TODO: Implementar firma real con certificado
        // Por ahora, simulamos copiando el mismo archivo
        String xmlContent = new String(storageService.downloadFile(xmlPath).readAllBytes(), StandardCharsets.UTF_8);

        // Simular firma agregando comentario
        String xmlFirmado = xmlContent.replace("</FacturaElectronica>",
            "<!-- Firmado digitalmente -->\n</FacturaElectronica>");

        // Guardar firmado
        String clave = extraerClaveDeKey(xmlPath);
        Factura factura = facturaRepository.findByClave(clave)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
        String keyFirmado = FacturaDocumento.generarS3Key(empresaNombre, clave, TipoArchivoFactura.XML_SIGNED);
        storageService.uploadFile(
            new ByteArrayInputStream(xmlFirmado.getBytes(StandardCharsets.UTF_8)),
            keyFirmado,
            "application/xml",
            xmlFirmado.length()
        );

        // Registrar documento firmado
        FacturaDocumento doc = new FacturaDocumento();
        doc.setClave(clave);
        doc.setFacturaId(obtenerFacturaIdPorClave(clave));
        doc.setTipoArchivo(TipoArchivoFactura.XML_SIGNED);
        doc.setS3Key(keyFirmado);
        doc.setS3Bucket("facturas");
        doc.setTamanio((long) xmlFirmado.length());
        documentoRepository.save(doc);

        return keyFirmado;
    }

    @Override
    public String enviarHacienda(String xmlFirmadoPath, String clave) {
        log.info("Enviando a Hacienda documento con clave: {}", clave);

        Factura factura = facturaRepository.findByClave(clave)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        EmpresaConfigHacienda config = factura.getSucursal().getEmpresa().getConfigHacienda();
        if (config == null) {
            throw new RuntimeException("Empresa no tiene configuración de Hacienda");
        }

        // Determinar URL según ambiente
        String urlApi = config.getAmbiente() == AmbienteHacienda.PRODUCCION
            ? urlHaciendaProduccion
            : urlHaciendaSandbox;

        log.info("Enviando a {} ({})", urlApi, config.getAmbiente());

        // TODO: Implementar llamada real a API de Hacienda
        // Necesitarás:
        // - Autenticación OAuth2 con usuario/clave de config
        // - Envío del XML firmado
        // - Manejo de respuesta

        // Por ahora, simulamos respuesta exitosa
        String respuestaXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <MensajeHacienda>
                <Clave>%s</Clave>
                <Estado>ACEPTADO</Estado>
                <FechaEmision>%s</FechaEmision>
            </MensajeHacienda>
            """.formatted(clave, LocalDateTime.now());

        // Guardar respuesta
        String keyRespuesta = FacturaDocumento.generarS3Key(
            factura.getSucursal().getEmpresa().getNombreComercial(),
            clave,
            TipoArchivoFactura.XML_RESPUESTA);
        storageService.uploadFile(
            new ByteArrayInputStream(respuestaXml.getBytes(StandardCharsets.UTF_8)),
            keyRespuesta,
            "application/xml",
            respuestaXml.length()
        );

        guardarDocumento(factura, TipoArchivoFactura.XML_RESPUESTA, keyRespuesta, respuestaXml.length());

        return respuestaXml;
    }

    @Override
    public String generarPDF(Long facturaId) {
        log.info("Generando PDF para factura: {}", facturaId);

        // TODO: Implementar generación real de PDF
        // Usar librerías como JasperReports o iText

        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        // Por ahora, un PDF simulado
        String pdfContent = "PDF simulado para factura: " + factura.getClave();

        String key = FacturaDocumento.generarS3Key(
            factura.getSucursal().getEmpresa().getNombreComercial(),
            factura.getClave(),
            TipoArchivoFactura.PDF_FACTURA);
        storageService.uploadFile(
            new ByteArrayInputStream(pdfContent.getBytes()),
            key,
            "application/pdf",
            pdfContent.length()
        );

        guardarDocumento(factura, TipoArchivoFactura.PDF_FACTURA, key, pdfContent.length());

        return key;
    }

    @Override
    public void enviarEmail(Long facturaId, String pdfPath, String xmlPath) {
        log.info("Enviando email para factura: {}", facturaId);

        Factura factura = facturaRepository.findById(facturaId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        String emailsCliente = factura.getCliente().getEmails();
        if (emailsCliente == null || emailsCliente.trim().isEmpty()) {
            log.warn("Cliente {} no tiene emails registrados", factura.getCliente().getId());
            return;
        }

        // Parsear emails (separados por coma, máximo 4)
        String[] emails = emailsCliente.split(",");
        for (String email : emails) {
            String emailTrimmed = email.trim();
            if (!emailTrimmed.isEmpty()) {
                // TODO: Implementar envío real de email
                // Usar JavaMailSender o servicio externo (SendGrid, etc)
                log.info("Enviando factura {} a: {}", factura.getClave(), emailTrimmed);
            }
        }

        log.info("Emails enviados para factura: {} a {} destinatarios",
            factura.getClave(), emails.length);
    }

    // Métodos auxiliares privados

    private void procesarRespuestaHacienda(Factura factura, String respuestaXml) {
        // TODO: Parsear XML de respuesta y actualizar estado
        if (respuestaXml.contains("ACEPTADO")) {
            factura.setEstado(EstadoFactura.ACEPTADA);
        } else if (respuestaXml.contains("RECHAZADO")) {
            factura.setEstado(EstadoFactura.RECHAZADA);
        }
        facturaRepository.save(factura);
    }

    private void guardarDocumento(Factura factura, TipoArchivoFactura tipo, String s3Key, long tamanio) {
        FacturaDocumento doc = new FacturaDocumento();
        doc.setClave(factura.getClave());
        doc.setFacturaId(factura.getId());
        doc.setTipoArchivo(tipo);
        doc.setS3Key(s3Key);
        doc.setS3Bucket("facturas");
        doc.setTamanio(tamanio);
        documentoRepository.save(doc);
    }

    private String obtenerArchivoPath(String clave, TipoArchivoFactura tipo) {
        return documentoRepository.findByClaveAndTipoArchivo(clave, tipo)
            .map(FacturaDocumento::getS3Key)
            .orElseThrow(() -> new RuntimeException("Archivo no encontrado: " + tipo));
    }

    private String extraerClaveDeKey(String s3Key) {
        // Extraer clave del path: facturas/2025/08/09/CLAVE_TIPO.ext
        String[] partes = s3Key.split("/");
        String archivo = partes[partes.length - 1];
        return archivo.substring(0, 50); // Los primeros 50 caracteres son la clave
    }

    private Long obtenerFacturaIdPorClave(String clave) {
        return facturaRepository.findByClave(clave)
            .map(Factura::getId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada con clave: " + clave));
    }
}