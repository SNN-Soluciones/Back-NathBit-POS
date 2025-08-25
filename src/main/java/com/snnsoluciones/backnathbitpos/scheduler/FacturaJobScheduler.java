package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaAsyncProcessor;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.util.FacturaFirmaService;
import com.snnsoluciones.backnathbitpos.util.FacturaXMLGeneratorService;
import com.snnsoluciones.backnathbitpos.util.HaciendaAuthException;
import com.snnsoluciones.backnathbitpos.util.HaciendaClientService;
import com.snnsoluciones.backnathbitpos.util.HaciendaResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FacturaJobScheduler {

    private final FacturaJobService jobService;
    private final FacturaAsyncProcessor asyncProcessor;
    private final FacturaRepository facturaRepository;
    private final EmpresaConfigHaciendaRepository configRepository;
    private final StorageService storageService;
    private final FacturaXMLGeneratorService xmlGenerator;
    private final FacturaFirmaService firmaService;
    private final HaciendaClientService haciendaClient;

    @Value("${factura.processor.enabled:true}")
    private boolean processorEnabled;

    @Value("${factura.processor.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelay = 60000) // Cada minuto
    public void procesarFacturasPendientes() {
        if (!processorEnabled) {
            log.debug("Procesador de facturas DESHABILITADO");
            return;
        }

        log.info("============================================================");
        log.info("INICIANDO PROCESAMIENTO DE FACTURAS - {}", LocalDateTime.now());
        log.info("============================================================");

        try {
            // Obtener jobs pendientes
            List<FacturaJob> jobsPendientes = jobService.obtenerJobsPendientes(batchSize);
            log.info("Jobs pendientes encontrados: {}", jobsPendientes.size());

            if (jobsPendientes.isEmpty()) {
                log.info("No hay jobs pendientes para procesar");
                return;
            }

            // Procesar cada job
            for (FacturaJob job : jobsPendientes) {
                try {
                    log.info("------------------------------------------------------------");
                    log.info("PROCESANDO JOB ID: {} | Clave: {} | Paso: {} | Intento: #{}",
                        job.getId(), job.getClave(), job.getPasoActual(), job.getIntentos() + 1);

                    procesarJob(job);

                } catch (Exception e) {
                    log.error("ERROR procesando job {}: {}", job.getId(), e.getMessage());
                    jobService.marcarError(job.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("ERROR GENERAL en scheduler: {}", e.getMessage(), e);
        } finally {
            log.info("============================================================");
            log.info("FIN PROCESAMIENTO - {}", LocalDateTime.now());
            log.info("============================================================\n");
        }
    }

    @Transactional // CRÍTICO: Necesario para mantener la sesión de Hibernate activa
    protected void procesarJob(FacturaJob job) {
        // Obtener factura con fetch join para evitar lazy loading
        Factura factura = facturaRepository.findByIdWithRelaciones(job.getFacturaId())
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + job.getFacturaId()));

        log.info("Factura encontrada - ID: {} | Tipo: {} | Cliente: {} | Total: {}",
            factura.getId(),
            factura.getTipoDocumento(),
            factura.getCliente() != null ? factura.getCliente().getRazonSocial() : "CONSUMIDOR FINAL",
            factura.getTotalComprobante());

        // Obtener configuración de Hacienda - Ya tenemos la empresa cargada por el fetch join
        Long empresaId = factura.getSucursal().getEmpresa().getId();
        EmpresaConfigHacienda config = configRepository.findByEmpresaId(empresaId)
            .orElseThrow(() -> new RuntimeException("Config Hacienda no encontrada para empresa: " + empresaId));

        log.info("Config Hacienda - Empresa: {} | Ambiente: {} | Usuario: {}",
            factura.getSucursal().getEmpresa().getNombreComercial(),
            config.getAmbiente(),
            config.getUsuarioHacienda());

        // Marcar como procesando
        job.setEstadoProceso(EstadoProcesoJob.PROCESANDO);
        jobService.marcarError(job.getId(), null); // Solo actualiza estado

        switch (job.getPasoActual()) {
            case GENERAR_XML -> procesarGeneracionXML(job, factura);
            case FIRMAR_DOCUMENTO -> procesarFirmaXML(job, factura, config);
            case ENVIAR_HACIENDA -> procesarEnvioHacienda(job, factura, config);
            case GENERAR_PDF -> procesarGeneracionPDF(job, factura);
            case ENVIAR_EMAIL -> procesarEnvioEmail(job, factura);
            default -> log.warn("Paso no reconocido: {}", job.getPasoActual());
        }
    }

    private void procesarGeneracionXML(FacturaJob job, Factura factura) {
        log.info(">>> PASO 1: GENERANDO XML para factura {}", factura.getId());

        try {
            // Generar XML
            log.info("Invocando generador XML...");
            String xmlContent = xmlGenerator.generarXML(factura.getId());
            log.info("XML generado exitosamente - Tamaño: {} caracteres", xmlContent.length());

            // Guardar en S3
            String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
            String s3Key = String.format("facturas/%s/%s/%s.xml",
                empresaNombre.replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDateTime.now().toLocalDate(),
                job.getClave()
            );

            try (InputStream xmlStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
                byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
                String s3Path = storageService.uploadFile(xmlStream, s3Key, "application/xml", xmlBytes.length);
                log.info("XML guardado en S3: {}", s3Path);

                // Actualizar job
                job.setPasoActual(PasoFacturacion.FIRMAR_DOCUMENTO);
                job.setXmlPath(s3Path);
                jobService.marcarError(job.getId(), null); // Solo actualiza estado

            } catch (Exception e) {
                throw new RuntimeException("Error guardando XML en S3", e);
            }

        } catch (Exception e) {
            log.error("ERROR generando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando XML", e);
        }
    }

    private void procesarFirmaXML(FacturaJob job, Factura factura, EmpresaConfigHacienda config) {
        log.info(">>> PASO 2: FIRMANDO XML para factura {}", factura.getId());

        try {
            // El servicio de firma espera la ruta del XML en S3 y el ID de empresa
            String xmlFirmadoPath = firmaService.firmarXML(
                job.getXmlPath(),
                factura.getSucursal().getEmpresa().getId()
            );

            log.info("XML firmado exitosamente y guardado en: {}", xmlFirmadoPath);

            // Actualizar job con la ruta del XML firmado
            job.setPasoActual(PasoFacturacion.ENVIAR_HACIENDA);
            job.setXmlPathSigned(xmlFirmadoPath);
            jobService.marcarError(job.getId(), null);

        } catch (Exception e) {
            log.error("ERROR firmando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error firmando XML", e);
        }
    }

    private void procesarEnvioHacienda(FacturaJob job, Factura factura, EmpresaConfigHacienda config) {
        log.info(">>> PASO 3: ENVIANDO A HACIENDA factura {}", factura.getId());

        try {
            // Obtener XML firmado
            String xmlFirmado = storageService.downloadFileAsString(job.getXmlPathSigned());

            // Enviar a Hacienda
            HaciendaResponse response = haciendaClient.enviarDocumento(
                xmlFirmado,
                job.getClave(),
                config
            );

            log.info("Respuesta Hacienda - Estado: {} | Respuesta: {}",
                response.getIndEstado(), response.getRespuestaXml());

            // Guardar respuesta
            String s3KeyRespuesta = job.getXmlPathSigned().replace(".xml", "_respuesta.xml");
            try (InputStream respStream = new ByteArrayInputStream(response.getRespuestaXml().getBytes(StandardCharsets.UTF_8))) {
                byte[] respBytes = response.getRespuestaXml().getBytes(StandardCharsets.UTF_8);
                storageService.uploadFile(respStream, s3KeyRespuesta, "application/xml", respBytes.length);
            }

            // Actualizar estado según respuesta
            if (response.isAceptado()) {
                log.info("FACTURA ACEPTADA POR HACIENDA");
                factura.setEstado(EstadoFactura.ACEPTADA);
                job.setPasoActual(PasoFacturacion.GENERAR_PDF);
            } else if (response.isRechazado()) {
                log.warn("FACTURA RECHAZADA POR HACIENDA");
                factura.setEstado(EstadoFactura.RECHAZADA);
                job.setEstadoProceso(EstadoProcesoJob.ERROR);
                // TODO: Notificar rechazo
            } else {
                log.info("Factura en proceso en Hacienda, reintentando luego...");
                // No cambiar paso, reintentar después
            }

            facturaRepository.save(factura);
            jobService.marcarError(job.getId(), null);

        } catch (HaciendaAuthException e) {
            log.error("ERROR 403 - Problemas de autenticación con Hacienda: {}", e.getMessage());
            // TODO: Enviar email de alerta
            throw e;
        } catch (Exception e) {
            log.error("ERROR enviando a Hacienda: {}", e.getMessage(), e);
            throw new RuntimeException("Error enviando a Hacienda", e);
        }
    }

    private void procesarGeneracionPDF(FacturaJob job, Factura factura) {
        log.info(">>> PASO 4: GENERANDO PDF para factura {} (próximamente...)", factura.getId());
        // TODO: Implementar
        job.setPasoActual(PasoFacturacion.ENVIAR_EMAIL);
        jobService.marcarError(job.getId(), null);
    }

    private void procesarEnvioEmail(FacturaJob job, Factura factura) {
        log.info(">>> PASO 5: ENVIANDO EMAIL para factura {} (próximamente...)", factura.getId());
        // TODO: Implementar
        job.setPasoActual(PasoFacturacion.COMPLETADO);
        jobService.marcarCompletado(job.getId());
        log.info("¡¡¡PROCESO COMPLETADO EXITOSAMENTE!!!");
    }
}