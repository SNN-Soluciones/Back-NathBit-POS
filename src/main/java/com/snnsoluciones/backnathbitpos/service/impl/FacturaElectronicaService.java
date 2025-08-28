package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.factura.CrearBitacoraDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaBitacoraDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FiltrosBitacoraDto;
import com.snnsoluciones.backnathbitpos.dto.factura.NotificacionErrorDto;
import com.snnsoluciones.backnathbitpos.dto.factura.ReprocesarFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.factura.ResultadoProcesamientoDto;
import com.snnsoluciones.backnathbitpos.dto.factura.ResumenBitacoraDto;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.TokenService;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.scheduler.FacturaXMLGeneratorService;
import com.snnsoluciones.backnathbitpos.service.*;
import com.snnsoluciones.backnathbitpos.service.pdf.PdfGeneratorService;
import com.snnsoluciones.backnathbitpos.util.FacturaFirmaService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Servicio principal para procesamiento de facturación electrónica
 * Maneja todo el flujo: XML -> Firma -> Hacienda -> PDF -> Email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaElectronicaService {

    private final FacturaBitacoraRepository bitacoraRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaXMLGeneratorService xmlGenerator;
    private final FacturaFirmaService firmaService;
    private final StorageService storageService;
    private final HaciendaClient haciendaClient;
    private final TokenService tokenService;
    private final PdfGeneratorService pdfGenerator;
    private final EmailService emailService;
    private final S3PathBuilder s3PathBuilder;

    @Value("${app.facturacion.max-intentos:3}")
    private int maxIntentos;

    @Value("${app.facturacion.batch-size:10}")
    private int batchSize;

    /**
     * Crea entrada en bitácora al generar una factura
     */
    @Transactional
    public FacturaBitacoraDto crearBitacora(CrearBitacoraDto dto) {
        log.info("Creando bitácora para factura: {}", dto.getFacturaId());
        
        // Validar que no exista
        if (bitacoraRepository.existsByFacturaId(dto.getFacturaId())) {
            throw new IllegalStateException("Ya existe bitácora para factura: " + dto.getFacturaId());
        }
        
        FacturaBitacora bitacora = new FacturaBitacora();
        bitacora.setFacturaId(dto.getFacturaId());
        bitacora.setClave(dto.getClave());
        bitacora.setEstado(EstadoBitacora.PENDIENTE);
        bitacora.setIntentos(0);
        bitacora.setCreatedAt(LocalDateTime.now());
        bitacora.setUpdatedAt(LocalDateTime.now());
        
        bitacora = bitacoraRepository.save(bitacora);
        return mapToDto(bitacora);
    }

    /**
     * Método principal - procesa facturas pendientes
     * Llamado por el @Scheduled job
     */
    @Transactional
    public void procesarFacturasPendientes() {
        log.info("Iniciando procesamiento de facturas pendientes...");
        
        List<FacturaBitacora> pendientes = bitacoraRepository.findPendientes(
            PageRequest.of(0, batchSize)
        );
        
        log.info("Facturas a procesar: {}", pendientes.size());
        
        for (FacturaBitacora bitacora : pendientes) {
            try {
                procesarFactura(bitacora.getId());
            } catch (Exception e) {
                log.error("Error procesando bitácora {}: {}", bitacora.getId(), e.getMessage());
                manejarError(bitacora, e);
            }
        }
    }

    /**
     * Procesa una factura específica - TODO EL FLUJO
     */
    @Transactional
    public void procesarFactura(Long bitacoraId) {
        log.info("Procesando factura con bitácoraId: {}", bitacoraId);
        
        FacturaBitacora bitacora = bitacoraRepository.findById(bitacoraId)
            .orElseThrow(() -> new IllegalArgumentException("Bitácora no encontrada: " + bitacoraId));
        
        Factura factura = facturaRepository.findById(bitacora.getFacturaId())
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + bitacora.getFacturaId()));
        
        try {
            // Marcar como procesando
            bitacora.setEstado(EstadoBitacora.PROCESANDO);
            bitacora.setUpdatedAt(LocalDateTime.now());
            bitacoraRepository.save(bitacora);
            
            // 1. GENERAR XML
            String xmlPath = generarXML(factura, bitacora);
            
            // 2. FIRMAR XML
            String xmlFirmadoPath = firmarXML(factura, bitacora, xmlPath);
            
            // 3. ENVIAR A HACIENDA
            enviarHacienda(factura, bitacora, xmlFirmadoPath);
            
            // 4. ESPERAR RESPUESTA (polling o inmediata)
            boolean aceptada = consultarEstado(factura, bitacora);
            
            if (aceptada) {
                // 5. GENERAR PDF Y ENVIAR EMAIL
                generarPDFyEnviarEmail(factura, bitacora);
                
                // 6. MARCAR COMO COMPLETADO
                bitacora.setEstado(EstadoBitacora.ACEPTADA);
                bitacora.setProcesadoAt(LocalDateTime.now());
                factura.setEstado(EstadoFactura.ACEPTADA);
            } else {
                // RECHAZADA
                bitacora.setEstado(EstadoBitacora.RECHAZADA);
                factura.setEstado(EstadoFactura.RECHAZADA);
                notificarRechazo(factura, bitacora);
            }
            
            bitacora.setUpdatedAt(LocalDateTime.now());
            bitacoraRepository.save(bitacora);
            facturaRepository.save(factura);
            
            log.info("Factura {} procesada exitosamente con estado: {}", 
                factura.getClave(), bitacora.getEstado());
            
        } catch (Exception e) {
            log.error("Error procesando factura {}: {}", factura.getClave(), e.getMessage(), e);
            manejarError(bitacora, e);
            throw e;
        }
    }

    /**
     * 1. Generar XML
     */
    private String generarXML(Factura factura, FacturaBitacora bitacora) {
        log.info("Generando XML para factura: {}", factura.getClave());
        
        try {
            String xmlContent = xmlGenerator.generarXML(factura.getId());
            
            // Guardar en S3
            String s3Key = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_UNSIGNED);
            storageService.uploadFile(
                new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)),
                s3Key,
                "application/xml",
                xmlContent.length()
            );
            
            bitacora.setXmlPath(s3Key);
            bitacoraRepository.save(bitacora);
            
            log.info("XML generado y guardado en: {}", s3Key);
            return s3Key;
            
        } catch (Exception e) {
            throw new RuntimeException("Error generando XML: " + e.getMessage(), e);
        }
    }

    /**
     * 2. Firmar XML
     */
    private String firmarXML(Factura factura, FacturaBitacora bitacora, String xmlPath) {
        log.info("Firmando XML para factura: {}", factura.getClave());
        
        try {
            // Descargar XML original
            byte[] xmlBytes = storageService.downloadFileAsBytes(xmlPath);
            String xmlContent = new String(xmlBytes, StandardCharsets.UTF_8);
            
            // Firmar
            Empresa empresa = factura.getSucursal().getEmpresa();
            String xmlFirmado = firmaService.firmarXML(
                xmlContent,
                empresa.getConfigHacienda().getP12Base64(),
                empresa.getConfigHacienda().getP12Clave()
            );
            
            // Guardar XML firmado
            String s3KeyFirmado = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_SIGNED);
            storageService.uploadFile(
                new ByteArrayInputStream(xmlFirmado.getBytes(StandardCharsets.UTF_8)),
                s3KeyFirmado,
                "application/xml",
                xmlFirmado.length()
            );
            
            bitacora.setXmlFirmadoPath(s3KeyFirmado);
            bitacoraRepository.save(bitacora);
            
            log.info("XML firmado y guardado en: {}", s3KeyFirmado);
            return s3KeyFirmado;
            
        } catch (Exception e) {
            throw new RuntimeException("Error firmando XML: " + e.getMessage(), e);
        }
    }

    /**
     * 3. Enviar a Hacienda
     */
    private void enviarHacienda(Factura factura, FacturaBitacora bitacora, String xmlFirmadoPath) {
        log.info("Enviando factura a Hacienda: {}", factura.getClave());
        
        try {
            // Preparar request
            byte[] xmlBytes = storageService.downloadFileAsBytes(xmlFirmadoPath);
            String xmlBase64 = Base64.getEncoder().encodeToString(xmlBytes);
            
            Empresa empresa = factura.getSucursal().getEmpresa();
            Cliente cliente = factura.getCliente();
            
            RecepcionRequest request = RecepcionRequest.builder()
                .clave(factura.getClave())
                .fecha(factura.getFechaEmision().toString())
                .comprobanteXml(xmlBase64)
                .build();
            
            // Obtener token
            HaciendaAuthParams auth = buildAuthParams(empresa);
            String token = tokenService.getValidToken(auth);
            
            // Enviar
            boolean sandbox = empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.SANDBOX;
            haciendaClient.enviarDocumento(request, token, sandbox);
            
            factura.setEstado(EstadoFactura.ENVIADA);
            facturaRepository.save(factura);
            
            log.info("Factura enviada exitosamente a Hacienda");
            
        } catch (Exception e) {
            throw new RuntimeException("Error enviando a Hacienda: " + e.getMessage(), e);
        }
    }

    /**
     * 4. Consultar estado en Hacienda
     */
    private boolean consultarEstado(Factura factura, FacturaBitacora bitacora) {
        log.info("Consultando estado en Hacienda para: {}", factura.getClave());
        
        try {
            Empresa empresa = factura.getSucursal().getEmpresa();
            HaciendaAuthParams auth = buildAuthParams(empresa);
            String token = tokenService.getValidToken(auth);
            boolean sandbox = empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.SANDBOX;
            
            // Intentar hasta 3 veces con espera
            int intentos = 0;
            while (intentos < 3) {
                ConsultaEstadoResponse respuesta = haciendaClient.getEstado(
                    token, sandbox, factura.getClave()
                );
                
                if (respuesta != null && respuesta.getIndEstado() != null) {
                    String estado = respuesta.getIndEstado().toLowerCase();
                    
                    // Guardar respuesta XML si existe
                    if (respuesta.getRespuestaXmlBase64() != null) {
                        guardarRespuestaXML(factura, bitacora, respuesta.getRespuestaXmlBase64());
                    }
                    
                    bitacora.setHaciendaMensaje(respuesta.getDetalleMensaje());
                    bitacoraRepository.save(bitacora);
                    
                    if (estado.contains("acept")) {
                        log.info("Factura ACEPTADA por Hacienda");
                        return true;
                    } else if (estado.contains("rechaz")) {
                        log.warn("Factura RECHAZADA por Hacienda: {}", respuesta.getDetalleMensaje());
                        return false;
                    }
                }
                
                // Si está procesando, esperar
                intentos++;
                if (intentos < 3) {
                    Thread.sleep(5000); // Esperar 5 segundos
                }
            }
            
            // Si después de 3 intentos no hay respuesta clara, programar para después
            bitacora.setProximoIntento(LocalDateTime.now().plusMinutes(5));
            bitacoraRepository.save(bitacora);
            throw new RuntimeException("Hacienda aún procesando - reprogramado para 5 minutos");
            
        } catch (Exception e) {
            throw new RuntimeException("Error consultando estado: " + e.getMessage(), e);
        }
    }

    /**
     * 5. Generar PDF y enviar email
     */
    private void generarPDFyEnviarEmail(Factura factura, FacturaBitacora bitacora) {
        log.info("Generando PDF y enviando email para: {}", factura.getClave());
        
        try {
            // Generar PDF (no guardar, solo en memoria)
            byte[] pdfBytes = pdfGenerator.generarPdf(factura);
            
            // Preparar attachments
            EmailFacturaDto emailDto = EmailFacturaDto.builder()
                .facturaId(factura.getId())
                .emailDestino(factura.getCliente().getEmails())
                .asunto("Factura Electrónica - " + factura.getClave())
                .cuerpo(buildEmailBody(factura))
                .pdfBytes(pdfBytes)
                .xmlBytes(storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath()))
                .build();
            
            // Enviar
            emailService.enviarFacturaElectronica(emailDto);
            
            log.info("Email enviado exitosamente a: {}", factura.getCliente().getEmails());
            
        } catch (Exception e) {
            log.error("Error enviando email: {}", e.getMessage());
            // No fallar el proceso por error de email
        }
    }

    /**
     * Manejo de errores con reintentos
     */
    private void manejarError(FacturaBitacora bitacora, Exception e) {
        bitacora.setIntentos(bitacora.getIntentos() + 1);
        bitacora.setUltimoError(e.getMessage());
        bitacora.setUpdatedAt(LocalDateTime.now());
        
        if (bitacora.getIntentos() >= maxIntentos) {
            bitacora.setEstado(EstadoBitacora.ERROR);
            notificarErrorSistema(bitacora, e);
        } else {
            // Backoff exponencial: 5min, 10min, 20min
            int minutosEspera = 5 * (int) Math.pow(2, bitacora.getIntentos() - 1);
            bitacora.setProximoIntento(LocalDateTime.now().plusMinutes(minutosEspera));
            bitacora.setEstado(EstadoBitacora.PENDIENTE);
        }
        
        bitacoraRepository.save(bitacora);
    }

    /**
     * Notificar rechazo al emisor
     */
    @Async
    private void notificarRechazo(Factura factura, FacturaBitacora bitacora) {
        try {
            Empresa empresa = factura.getSucursal().getEmpresa();
            String email = empresa.getEmail() != null ? empresa.getEmail() : empresa.getUsuarioEmpresas().stream()
                .filter(u -> u.getUsuario().getRol().getDisplayName().equals("ADMIN"))
                .findFirst()
                .map(Usuario::getEmail)
                .orElse(null);
            
            if (email != null) {
                NotificacionErrorDto notif = NotificacionErrorDto.builder()
                    .tipoError("HACIENDA_RECHAZO")
                    .facturaId(factura.getId())
                    .clave(factura.getClave())
                    .mensaje("Factura rechazada por Hacienda")
                    .detalleError(bitacora.getHaciendaMensaje())
                    .emailDestinatario(email)
                    .nombreEmpresa(empresa.getNombreComercial())
                    .fechaError(LocalDateTime.now())
                    .build();
                
                emailService.enviarNotificacionError(notif);
            }
        } catch (Exception e) {
            log.error("Error enviando notificación de rechazo: {}", e.getMessage());
        }
    }

    /**
     * Notificar error del sistema a soporte
     */
    @Async
    private void notificarErrorSistema(FacturaBitacora bitacora, Exception error) {
        try {
            NotificacionErrorDto notif = NotificacionErrorDto.builder()
                .tipoError("SISTEMA")
                .facturaId(bitacora.getFacturaId())
                .clave(bitacora.getClave())
                .mensaje("Error procesando factura después de " + maxIntentos + " intentos")
                .detalleError(error.getMessage())
                .emailDestinatario("info@snnsoluciones.com")
                .fechaError(LocalDateTime.now())
                .build();
            
            emailService.enviarNotificacionError(notif);
        } catch (Exception e) {
            log.error("Error enviando notificación a soporte: {}", e.getMessage());
        }
    }

    /**
     * Consultar bitácoras con filtros
     */
    @Transactional(readOnly = true)
    public Page<FacturaBitacoraDto> buscarBitacoras(FiltrosBitacoraDto filtros) {
        // Implementar búsqueda con Specification o Query personalizada
        PageRequest pageable = PageRequest.of(
            filtros.getPage(),
            filtros.getSize(),
            Sort.by(Sort.Direction.fromString(filtros.getSortDirection()), filtros.getSortBy())
        );
        
        Page<FacturaBitacora> page = bitacoraRepository.buscarConFiltros(
            filtros.getEstado(),
            filtros.getEmpresaId(),
            filtros.getSucursalId(),
            filtros.getFechaDesde(),
            filtros.getFechaHasta(),
            pageable
        );
        
        return page.map(this::mapToDto);
    }

    /**
     * Obtener resumen/dashboard
     */
    @Transactional(readOnly = true)
    public ResumenBitacoraDto obtenerResumen(Long empresaId) {
        return bitacoraRepository.obtenerResumen(empresaId);
    }

    /**
     * Reprocesar factura manualmente
     */
    @Transactional
    public ResultadoProcesamientoDto reprocesarFactura(ReprocesarFacturaDto dto) {
        FacturaBitacora bitacora = bitacoraRepository.findById(dto.getBitacoraId())
            .orElseThrow(() -> new IllegalArgumentException("Bitácora no encontrada"));
        
        // Reset para reproceso
        bitacora.setEstado(EstadoBitacora.PENDIENTE);
        bitacora.setIntentos(dto.getForzar() ? 0 : bitacora.getIntentos());
        bitacora.setProximoIntento(null);
        bitacora.setUltimoError(null);
        bitacora.setUpdatedAt(LocalDateTime.now());
        bitacoraRepository.save(bitacora);
        
        // Procesar inmediatamente
        procesarFactura(bitacora.getId());
        
        // Retornar resultado
        bitacora = bitacoraRepository.findById(dto.getBitacoraId()).orElseThrow();
        return buildResultado(bitacora);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private HaciendaAuthParams buildAuthParams(Empresa empresa) {
        return HaciendaAuthParams.builder()
            .empresaId(empresa.getId())
            .sandbox(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.SANDBOX)
            .clientId(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.SANDBOX ? "api-stag" : "api-prod")
            .username(empresa.getConfigHacienda().getUsuarioHacienda())
            .password(empresa.getConfigHacienda().getClaveHacienda())
            .build();
    }

    private void guardarRespuestaXML(Factura factura, FacturaBitacora bitacora, String xmlBase64) {
        try {
            byte[] xmlBytes = Base64.getDecoder().decode(xmlBase64);
            String s3Key = s3PathBuilder.buildXmlPath(factura, TipoArchivoFactura.XML_RESPUESTA);
            
            storageService.uploadFile(
                new ByteArrayInputStream(xmlBytes),
                s3Key,
                "application/xml",
                xmlBytes.length
            );
            
            bitacora.setXmlRespuestaPath(s3Key);
            bitacoraRepository.save(bitacora);
        } catch (Exception e) {
            log.error("Error guardando respuesta XML: {}", e.getMessage());
        }
    }

    private String buildEmailBody(Factura factura) {
        return String.format(
            """
            Estimado(a) %s,
            
            Le enviamos su factura electrónica #%s por un monto de %s %,.2f.
            
            Adjunto encontrará:
            - Factura en formato PDF
            - Comprobante XML firmado digitalmente
            
            Gracias por su preferencia.
            
            %s
            %s
            """,
            factura.getCliente().getRazonSocial(),
            factura.getClave(),
            factura.getMoneda().getCodigo(),
            factura.getTotalComprobante(),
            factura.getSucursal().getEmpresa().getNombreComercial(),
            factura.getSucursal().getNombre()
        );
    }

    private FacturaBitacoraDto mapToDto(FacturaBitacora entity) {
        // Implementar mapeo completo
        return FacturaBitacoraDto.builder()
            .id(entity.getId())
            .facturaId(entity.getFacturaId())
            .clave(entity.getClave())
            .estado(entity.getEstado())
            .intentos(entity.getIntentos())
            .proximoIntento(entity.getProximoIntento())
            .xmlPath(entity.getXmlPath())
            .xmlFirmadoPath(entity.getXmlFirmadoPath())
            .xmlRespuestaPath(entity.getXmlRespuestaPath())
            .haciendaMensaje(entity.getHaciendaMensaje())
            .ultimoError(entity.getUltimoError())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .procesadoAt(entity.getProcesadoAt())
            .build();
    }

    private ResultadoProcesamientoDto buildResultado(FacturaBitacora bitacora) {
        return ResultadoProcesamientoDto.builder()
            .bitacoraId(bitacora.getId())
            .facturaId(bitacora.getFacturaId())
            .clave(bitacora.getClave())
            .estado(bitacora.getEstado())
            .mensaje(bitacora.getHaciendaMensaje())
            .procesadoAt(bitacora.getProcesadoAt())
            .xmlUrl("/api/facturas/documento/" + bitacora.getId() + "/xml")
            .xmlFirmadoUrl("/api/facturas/documento/" + bitacora.getId() + "/xml-firmado")
            .xmlRespuestaUrl("/api/facturas/documento/" + bitacora.getId() + "/xml-respuesta")
            .build();
    }
}