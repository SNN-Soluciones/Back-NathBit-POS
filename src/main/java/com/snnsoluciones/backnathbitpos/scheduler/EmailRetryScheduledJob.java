//package com.snnsoluciones.backnathbitpos.scheduler;
//
//import com.snnsoluciones.backnathbitpos.dto.email.EmailFacturaDto;
//import com.snnsoluciones.backnathbitpos.entity.Factura;
//import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
//import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
//import com.snnsoluciones.backnathbitpos.repository.EmailAuditLogRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository.BitacoraMin;
//import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
//import com.snnsoluciones.backnathbitpos.service.EmailService;
//import com.snnsoluciones.backnathbitpos.service.StorageService;
//import com.snnsoluciones.backnathbitpos.service.pdf.FacturaPdfService;
//import jakarta.transaction.Transactional;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class EmailRetryScheduledJob {
//
//    private final FacturaBitacoraRepository bitacoraRepository;
//    private final FacturaRepository facturaRepository;
//    private final EmailAuditLogRepository emailAuditLogRepository;
//    private final EmailService emailService;
//    private final StorageService storageService;
//    private final FacturaPdfService pdfService;
//
//    // ===== Elige UNA de las dos opciones =====
//    // Opción A: ejecuta una sola vez al iniciar
//    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
//    @Transactional
//    public void runOnceOnStartup() { ejecutarProceso(); }
//
//    // Opción B: ejecuta cada 15 min (comentar si no lo usas)
//    // @Scheduled(cron = "0 */15 * * * *")
//    // @Transactional
//    public void runPeriodic() { ejecutarProceso(); }
//
//    // -----------------------------------------
//
//    private void ejecutarProceso() {
//        log.info("🔄 ===== INICIANDO ENVÍO PENDIENTE DE FACTURAS (Tipo 01) =====");
//        final LocalDateTime fechaDesde = LocalDate.of(2024, 10, 2).atStartOfDay();
//        final LocalDateTime fechaHasta = LocalDateTime.now();
//        log.info("📅 Rango: {} → {}", fechaDesde, fechaHasta);
//
//        // 1) Bitácoras candidatas (solo lo mínimo necesario)
//        List<BitacoraMin> candidatas =
//            bitacoraRepository.findAceptadasTipoFacturaBetween(fechaDesde, fechaHasta);
//        if (candidatas.isEmpty()) {
//            log.info("✅ No hay bitácoras ACEPTADAS de Factura Electrónica en el rango.");
//            return;
//        }
//        log.info("🔍 Candidatas encontradas: {}", candidatas.size());
//
//        // 2) Quitar las que ya tienen email ENVIADO
//        Set<Long> facturaIds = candidatas.stream().map(FacturaBitacoraRepository.BitacoraMin::getFacturaId).collect(Collectors.toSet());
//        Set<Long> yaEnviadas = emailAuditLogRepository.findFacturaIdsEnviados(facturaIds);
//        List<FacturaBitacoraRepository.BitacoraMin> porEnviar = candidatas.stream()
//            .filter(b -> !yaEnviadas.contains(b.getFacturaId()))
//            .toList();
//
//        log.info("📬 Por enviar (sin registro ENVIADO en audit): {}", porEnviar.size());
//        if (porEnviar.isEmpty()) {
//            log.info("✅ Nada pendiente por enviar.");
//            return;
//        }
//
//        // 3) Cargar Facturas con empresa/sucursal en un solo round-trip
//        Map<Long, Factura> facturasById = facturaRepository.findAllByIdInWithEmpresa(
//                porEnviar.stream().map(FacturaBitacoraRepository.BitacoraMin::getFacturaId).toList()
//            )
//            .stream().collect(Collectors.toMap(Factura::getId, f -> f));
//
//        int enviados = 0, omitidos = 0, errores = 0;
//
//        // 4) Procesar en lotes (simple)
//        for (var b : porEnviar) {
//            Factura factura = facturasById.get(b.getFacturaId());
//            if (factura == null) {
//                omitidos++;
//                log.warn("⚠️ Factura {} no encontrada, omitiendo.", b.getFacturaId());
//                continue;
//            }
//            // idempotencia adicional: verifica de nuevo por si otro proceso ya lo envió
//            if (emailAuditLogRepository.existsByFacturaIdAndEstado(factura.getId(), EstadoEmail.ENVIADO)) {
//                omitidos++;
//                continue;
//            }
//            try {
//                // validaciones mínimas
//                if (factura.getEmailReceptor() == null || factura.getEmailReceptor().isBlank()) {
//                    omitidos++;
//                    log.info("ℹ️ Factura {} sin emailReceptor, se omite.", factura.getClave());
//                    continue;
//                }
//                if (!TipoDocumento.FACTURA_ELECTRONICA.equals(factura.getTipoDocumento())) {
//                    omitidos++;
//                    log.info("ℹ️ Factura {} no es tipo 01, se omite.", factura.getClave());
//                    continue;
//                }
//
//                // Generar PDF en el momento (puedes cachear si lo deseas)
//                byte[] pdfBytes = pdfService.generarFacturaCarta(factura.getClave());
//
//                // Enviar
//                enviarEmail(b, factura, pdfBytes);
//                enviados++;
//
//            } catch (Exception ex) {
//                errores++;
//                log.error("❌ Error enviando factura {}: {}", factura.getClave(), ex.getMessage(), ex);
//            }
//        }
//
//        log.info("✅ ===== RESUMEN EMAILS FACTURAS =====");
//        log.info("   Total candidatas: {}", candidatas.size());
//        log.info("   Ya enviadas (audit): {}", yaEnviadas.size());
//        log.info("   Procesadas (pendientes): {}", porEnviar.size());
//        log.info("   Enviadas: {}", enviados);
//        log.info("   Omitidas: {}", omitidos);
//        log.info("   Errores: {}", errores);
//    }
//
//    /**
//     * Versión que reutiliza objetos ya cargados y bytes de PDF.
//     */
//    private void enviarEmail(FacturaBitacoraRepository.BitacoraMin bitacora, Factura factura, byte[] pdfBytes) {
//        try {
//            // Adjuntos XML (si hay paths)
//            byte[] xmlFirmadoBytes = null;
//            if (bitacora.getXmlFirmadoPath() != null) {
//                xmlFirmadoBytes = storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath());
//            }
//            byte[] respuestaHaciendaBytes = null;
//            if (bitacora.getXmlRespuestaPath() != null) {
//                respuestaHaciendaBytes = storageService.downloadFileAsBytes(bitacora.getXmlRespuestaPath());
//            }
//
//            EmailFacturaDto dto = EmailFacturaDto.builder()
//                .facturaId(factura.getId())
//                .clave(factura.getClave())
//                .consecutivo(factura.getConsecutivo())
//                .emailDestino(factura.getEmailReceptor())
//                .tipoDocumento(factura.getTipoDocumento().getDescripcion())
//                .nombreComercial(factura.getSucursal().getEmpresa().getNombreComercial())
//                .razonSocial(factura.getSucursal().getEmpresa().getNombreRazonSocial())
//                .cedulaJuridica(factura.getSucursal().getEmpresa().getIdentificacion())
//                .fechaEmision(formatearFecha(factura.getFechaEmision()))
//                .logoUrl(factura.getSucursal().getEmpresa().getLogoUrl())
//                .pdfBytes(pdfBytes)
//                .xmlFirmadoBytes(xmlFirmadoBytes)
//                .respuestaHaciendaBytes(respuestaHaciendaBytes)
//                .build();
//
//            emailService.enviarFacturaElectronica(dto); // ← que esto cree el audit ENVIADO
//            log.info("📧 Email enviado a {} (clave: {})", factura.getEmailReceptor(), factura.getClave());
//
//        } catch (Exception e) {
//            throw new RuntimeException("Fallo envío email: " + e.getMessage(), e);
//        }
//    }
//
//    // Si tu fecha de emisión es String ISO (ZonedDateTime), mantenemos parseo robusto.
//    private String formatearFecha(String fechaStr) {
//        try {
//            if (fechaStr == null || fechaStr.isBlank()) {
//                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
//            }
//            ZonedDateTime zdt = ZonedDateTime.parse(fechaStr);
//            return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
//        } catch (DateTimeParseException e) {
//            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
//        }
//    }
//}