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
                    log.error("ERROR procesando job {}: {}", job.getId(), e.getMessage(), e);
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
    
    private void procesarJob(FacturaJob job) {
        // Obtener factura
        Factura factura = facturaRepository.findById(job.getFacturaId())
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + job.getFacturaId()));
            
        log.info("Factura encontrada - ID: {} | Tipo: {} | Cliente: {} | Total: {}", 
            factura.getId(), 
            factura.getTipoDocumento(),
            factura.getCliente() != null ? factura.getCliente().getRazonSocial() : "CONSUMIDOR FINAL",
            factura.getTotalComprobante());
        
        // Obtener configuración de Hacienda
        EmpresaConfigHacienda config = configRepository.findByEmpresaId(
            factura.getSucursal().getEmpresa().getId())
            .orElseThrow(() -> new RuntimeException("Config Hacienda no encontrada"));
            
        log.info("Config Hacienda - Empresa: {} | Ambiente: {} | Usuario: {}", 
            config.getEmpresa().getNombreComercial(),
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
            String s3Key = String.format("empresas/%s/%s/xml_unsigned.xml", 
                empresaNombre, factura.getClave());
            
            log.info("Guardando XML en S3 - Key: {}", s3Key);
            storageService.uploadFile(
                new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)),
                s3Key,
                "application/xml",
                xmlContent.length()
            );
            log.info("XML guardado exitosamente en S3");
            
            // Actualizar job
            job.setPasoActual(PasoFacturacion.FIRMAR_DOCUMENTO);
            jobService.marcarError(job.getId(), null);
            log.info("Job actualizado al siguiente paso: FIRMAR_DOCUMENTO");
            
        } catch (Exception e) {
            log.error("ERROR generando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando XML", e);
        }
    }
    
    private void procesarFirmaXML(FacturaJob job, Factura factura, EmpresaConfigHacienda config) {
        log.info(">>> PASO 2: FIRMANDO XML para factura {}", factura.getId());
        
        try {
            String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
            String xmlPath = String.format("empresas/%s/%s/xml_unsigned.xml", 
                empresaNombre, factura.getClave());
            
            log.info("Descargando XML desde S3 - Path: {}", xmlPath);
            log.info("Certificado ubicado en: {}", config.getUrlCertificadoKey());
            log.info("Tipo autenticación: {}", config.getTipoAutenticacion());
            
            // Firmar
            String xmlFirmadoPath = firmaService.firmarXML(xmlPath, 
                factura.getSucursal().getEmpresa().getId());
            
            log.info("XML firmado exitosamente - Path: {}", xmlFirmadoPath);
            
            // Actualizar job
            job.setPasoActual(PasoFacturacion.ENVIAR_HACIENDA);
            jobService.marcarError(job.getId(), null);
            log.info("Job actualizado al siguiente paso: ENVIAR_HACIENDA");
            
        } catch (Exception e) {
            log.error("ERROR firmando XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error firmando XML", e);
        }
    }
    
    private void procesarEnvioHacienda(FacturaJob job, Factura factura, EmpresaConfigHacienda config) {
        log.info(">>> PASO 3: ENVIANDO A HACIENDA factura {}", factura.getId());
        
        try {
            String empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
            String xmlFirmadoPath = String.format("empresas/%s/%s/xml_signed.xml", 
                empresaNombre, factura.getClave());
            
            log.info("Descargando XML firmado desde S3 - Path: {}", xmlFirmadoPath);
            
            // Descargar XML firmado
            String xmlFirmado;
            try (InputStream is = storageService.downloadFile(xmlFirmadoPath)) {
                xmlFirmado = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            log.info("XML firmado descargado - Tamaño: {} bytes", xmlFirmado.length());
            
            // Enviar a Hacienda
            log.info("Enviando a Hacienda - Ambiente: {} | Clave: {}", 
                config.getAmbiente(), factura.getClave());
            
            HaciendaResponse response = haciendaClient.enviarDocumento(
                xmlFirmado, factura.getClave(), config);
            
            log.info("Respuesta Hacienda - Estado: {} | Fecha: {}", 
                response.getIndEstado(), response.getFecha());
            
            if (response.isAceptado()) {
                log.info("¡¡¡FACTURA ACEPTADA POR HACIENDA!!!");
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