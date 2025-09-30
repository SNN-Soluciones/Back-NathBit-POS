package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/reportes/cierre-caja")
@RequiredArgsConstructor
@Tag(name = "Reportes de Caja", description = "Generación de reportes de cierre de caja")
public class ReporteCierreCajaController {

    private final SesionCajaService sesionCajaService;
    private final SecurityContextService securityContextService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Generar reporte de cierre de caja 80mm")
    @GetMapping("/{sesionId}/reporte-80mm")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<?> generarReporte80mm(
            @PathVariable Long sesionId,
            HttpServletRequest httpRequest) {

        try {
            // Verificar permisos
            String token = httpRequest.getHeader("Authorization").substring(7);
            Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

            // Obtener sesión
            SesionCaja sesion = sesionCajaService.buscarPorId(sesionId)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            // Validar permisos (cajero solo su propia sesión, supervisores cualquiera)
            if (!securityContextService.isSupervisor() &&
                    !sesion.getUsuario().getId().equals(usuarioId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("No tiene permisos para ver este reporte"));
            }

            // Validar que la sesión esté cerrada
            if (sesion.getEstado().name().equals("ABIERTA")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("La sesión debe estar cerrada para generar el reporte"));
            }

            // Obtener resumen detallado
            ResumenCajaDetalladoDTO resumen = sesionCajaService.obtenerResumenDetallado(sesion.getId());

            // Generar HTML del reporte
            String htmlReporte = generarHTML80mm(sesion, resumen);

            // Retornar HTML con headers apropiados
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                    .body(htmlReporte);

        } catch (Exception e) {
            log.error("Error generando reporte: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al generar reporte: " + e.getMessage()));
        }
    }

    private String generarHTML80mm(SesionCaja sesion, ResumenCajaDetalladoDTO resumen) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Calcular diferencia
        BigDecimal diferencia = sesion.getMontoCierre().subtract(resumen.getMontoEsperado());

        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Cierre de Caja</title>");
        html.append("<style>");
        // Estilos para impresora de 80mm
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body { font-family: 'Courier New', monospace; font-size: 11px; width: 80mm; margin: 0 auto; }");
        html.append(".header { text-align: center; margin-bottom: 10px; }");
        html.append(".title { font-size: 14px; font-weight: bold; margin: 5px 0; }");
        html.append(".subtitle { font-size: 12px; margin: 3px 0; }");
        html.append(".divider { border-bottom: 1px dashed #000; margin: 5px 0; }");
        html.append(".double-divider { border-bottom: 2px solid #000; margin: 8px 0; }");
        html.append(".row { display: flex; justify-content: space-between; margin: 2px 0; }");
        html.append(".label { text-align: left; }");
        html.append(".value { text-align: right; }");
        html.append(".section-title { font-weight: bold; margin: 8px 0 4px 0; text-align: center; }");
        html.append(".total-row { font-weight: bold; margin: 5px 0; }");
        html.append(".footer { text-align: center; margin-top: 15px; font-size: 10px; }");
        html.append("@media print { body { width: 100%; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<div class='header'>");
        html.append("<div class='title'>").append(sesion.getTerminal().getSucursal().getEmpresa().getNombreComercial()).append("</div>");
        html.append("<div class='subtitle'>").append(sesion.getTerminal().getSucursal().getNombre()).append("</div>");
        html.append("<div class='subtitle'>CIERRE DE CAJA</div>");
        html.append("</div>");

        html.append("<div class='divider'></div>");

        // Información básica
        html.append("<div class='row'><span>Terminal:</span><span>").append(sesion.getTerminal().getNombre()).append("</span></div>");
        html.append("<div class='row'><span>Cajero:</span><span>").append(sesion.getUsuario().getUsername()).append("</span></div>");
        html.append("<div class='row'><span>Apertura:</span><span>").append(sesion.getFechaHoraApertura().format(dateFormatter)).append("</span></div>");
        html.append("<div class='row'><span>Cierre:</span><span>").append(sesion.getFechaHoraCierre().format(dateFormatter)).append("</span></div>");

        html.append("<div class='double-divider'></div>");

        // Resumen de montos
        html.append("<div class='section-title'>RESUMEN DE MONTOS</div>");
        html.append("<div class='row'><span>Monto Inicial:</span><span>").append(currency.format(sesion.getMontoInicial())).append("</span></div>");
        html.append("<div class='row'><span>Total Ventas:</span><span>").append(currency.format(resumen.getMontoEsperado())).append("</span></div>");
        
        if (resumen.getTotalNotasCredito().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div class='row'><span>(-) Notas Crédito:</span><span>").append(currency.format(resumen.getTotalNotasCredito())).append("</span></div>");
        }
        
        if (resumen.getVales().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div class='row'><span>(-) Vales:</span><span>").append(currency.format(resumen.getVales())).append("</span></div>");
        }

        html.append("<div class='divider'></div>");
        html.append("<div class='row total-row'><span>Monto Esperado:</span><span>").append(currency.format(resumen.getMontoEsperado())).append("</span></div>");
        html.append("<div class='row total-row'><span>Monto de Cierre:</span><span>").append(currency.format(sesion.getMontoCierre())).append("</span></div>");
        html.append("<div class='divider'></div>");
        html.append("<div class='row total-row'><span>DIFERENCIA:</span><span>").append(currency.format(diferencia)).append("</span></div>");

        html.append("<div class='double-divider'></div>");

        // Desglose por tipo de pago
        html.append("<div class='section-title'>DESGLOSE POR TIPO DE PAGO</div>");
        html.append("<div class='row'><span>Efectivo:</span><span>").append(currency.format(resumen.getVentasEfectivo())).append("</span></div>");
        html.append("<div class='row'><span>Tarjeta:</span><span>").append(currency.format(resumen.getVentasTarjeta())).append("</span></div>");
        html.append("<div class='row'><span>Transferencia:</span><span>").append(currency.format(resumen.getVentasTransferencia())).append("</span></div>");
        if (resumen.getVentasOtros().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div class='row'><span>Otros:</span><span>").append(currency.format(resumen.getVentasOtros())).append("</span></div>");
        }

        html.append("<div class='double-divider'></div>");

        // Contadores de documentos
        html.append("<div class='section-title'>RESUMEN DE DOCUMENTOS</div>");
        html.append("<div class='row'><span>Facturas:</span><span>").append(resumen.getCantidadFacturas()).append(" (").append(currency.format(resumen.getTotalFacturas())).append(")</span></div>");
        html.append("<div class='row'><span>Tiquetes:</span><span>").append(resumen.getCantidadTiquetes()).append(" (").append(currency.format(resumen.getTotalTiquetes())).append(")</span></div>");
        
        if (resumen.getCantidadNotasCredito() > 0) {
            html.append("<div class='row'><span>Notas Crédito:</span><span>").append(resumen.getCantidadNotasCredito()).append(" (").append(currency.format(resumen.getTotalNotasCredito())).append(")</span></div>");
        }

        // Observaciones si existen
        if (sesion.getObservacionesCierre() != null && !sesion.getObservacionesCierre().isEmpty()) {
            html.append("<div class='double-divider'></div>");
            html.append("<div class='section-title'>OBSERVACIONES</div>");
            html.append("<div style='text-align: left; word-wrap: break-word; font-size: 10px;'>").append(sesion.getObservacionesCierre()).append("</div>");
        }

        // Footer
        html.append("<div class='footer'>");
        html.append("<div class='double-divider'></div>");
        html.append("<div style='margin-top: 20px;'>_______________________</div>");
        html.append("<div>Firma Cajero</div>");
        html.append("<div style='margin-top: 20px;'>_______________________</div>");
        html.append("<div>Firma Supervisor</div>");
        html.append("<div style='margin-top: 15px; font-size: 9px;'>Impreso: ").append(LocalDateTime.now().format(dateFormatter)).append("</div>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }
}