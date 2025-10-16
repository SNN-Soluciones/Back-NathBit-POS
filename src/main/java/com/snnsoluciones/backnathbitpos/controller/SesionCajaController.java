package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.*;
import com.snnsoluciones.backnathbitpos.dto.sesiones.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.RegistrarValeRequest;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.SesionCajaDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaDenominacionRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.MovimientoCajaService;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import com.snnsoluciones.backnathbitpos.service.pdf.PdfGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sesiones-caja")
@RequiredArgsConstructor
@Tag(name = "Sesiones de Caja", description = "Gestión de apertura y cierre de cajas")
public class SesionCajaController {

  private final SesionCajaService sesionCajaService;
  private final MovimientoCajaService movimientoCajaService;
  private final SecurityContextService securityContextService;
  private final JwtTokenProvider jwtTokenProvider;
  private final SesionCajaDenominacionRepository sesionCajaDenominacionRepository;
  private final PdfGeneratorService pdfGeneratorService;

  @Operation(summary = "Abrir sesión de caja")
  @PostMapping("/abrir")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> abrirSesion(
      @Valid @RequestBody AbrirSesionRequest request,
      HttpServletRequest httpRequest) {

    try {
      String token = httpRequest.getHeader("Authorization");
      token = token.substring(7, token.length());
      // Obtener usuario del JWT
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      // Verificar si ya tiene sesión abierta
      if (sesionCajaService.usuarioTieneSesionAbierta(usuarioId)) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Ya tienes una sesión de caja abierta"));
      }

      // Verificar si la terminal está ocupada
      if (sesionCajaService.terminalTieneSesionAbierta(request.getTerminalId())) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("La terminal ya tiene una sesión abierta"));
      }

      // Abrir sesión
      SesionCaja sesion = sesionCajaService.abrirSesion(
          usuarioId,
          request.getTerminalId(),
          request.getMontoInicial()
      );

      SesionCajaResponse response = construirResponse(sesion);

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesión de caja abierta exitosamente",
          response
      ));

    } catch (Exception e) {
      log.error("Error abriendo sesión: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al abrir sesión: " + e.getMessage()));
    }
  }

  // ==========================================
// ENDPOINTS DE IMPRESIÓN Y ENVÍO DE CIERRE
// ==========================================

  /**
   * 🖨️ Imprimir cierre de caja con opciones personalizadas
   */
  @PostMapping("/{sesionId}/cierre/imprimir")
  @PreAuthorize("hasAnyRole('ROOT', 'CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Imprimir cierre de caja con opciones personalizadas")
  public ResponseEntity<byte[]> imprimirCierreCaja(
      @PathVariable Long sesionId,
      @RequestBody OpcionesImpresionCierreDTO opciones) {

    log.info("🖨️ Generando PDF de cierre para sesión {} con opciones: {}", sesionId, opciones);

    try {
      byte[] pdfBytes = sesionCajaService.generarPdfCierre(sesionId, opciones);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(
          ContentDisposition.builder("inline")
              .filename("cierre_caja_" + sesionId + ".pdf")
              .build()
      );
      headers.setContentLength(pdfBytes.length);

      log.info("✅ PDF generado exitosamente para sesión {}", sesionId);

      return ResponseEntity.ok()
          .headers(headers)
          .body(pdfBytes);

    } catch (Exception e) {
      log.error("❌ Error generando PDF de cierre para sesión {}: {}", sesionId, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * 📧 Enviar cierre de caja por email
   */
  @PostMapping("/{sesionId}/cierre/enviar-email")
  @PreAuthorize("hasAnyRole('ROOT', 'CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Enviar cierre de caja por email")
  public ResponseEntity<ApiResponse<Void>> enviarCierrePorEmail(
      @PathVariable Long sesionId,
      @RequestBody OpcionesImpresionCierreDTO opciones) {

    log.info("📧 Enviando cierre de caja {} por email a: {}", sesionId, opciones.getCorreosAdicionales());

    try {
      sesionCajaService.enviarCierrePorEmail(sesionId, opciones);

      log.info("✅ Email de cierre enviado exitosamente para sesión {}", sesionId);

      return ResponseEntity.ok(
          ApiResponse.success("Email enviado exitosamente", null)
      );

    } catch (Exception e) {
      log.error("❌ Error enviando email de cierre para sesión {}: {}", sesionId, e.getMessage(), e);
      return ResponseEntity.ok(
          ApiResponse.error("Error al enviar email: " + e.getMessage())
      );
    }
  }

  /**
   * 🖨️📧 Imprimir Y enviar cierre de caja (acción combinada)
   */
  @PostMapping("/{sesionId}/cierre/imprimir-y-enviar")
  @PreAuthorize("hasAnyRole('ROOT', 'CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
  @Operation(summary = "Imprimir y enviar cierre de caja")
  public ResponseEntity<byte[]> imprimirYEnviarCierre(
      @PathVariable Long sesionId,
      @RequestBody OpcionesImpresionCierreDTO opciones) {

    log.info("🖨️📧 Imprimiendo y enviando cierre para sesión {} con opciones: {}", sesionId, opciones);

    try {
      // 1. Generar PDF
      byte[] pdfBytes = sesionCajaService.generarPdfCierre(sesionId, opciones);

      // 2. Enviar email de forma asíncrona (no bloqueante)
      CompletableFuture.runAsync(() -> {
        try {
          sesionCajaService.enviarCierrePorEmail(sesionId, opciones);
          log.info("✅ Email enviado exitosamente en background para sesión {}", sesionId);
        } catch (Exception e) {
          log.error("❌ Error enviando email en background: {}", e.getMessage(), e);
        }
      });

      // 3. Retornar PDF inmediatamente
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDisposition(
          ContentDisposition.builder("inline")
              .filename("cierre_caja_" + sesionId + ".pdf")
              .build()
      );
      headers.setContentLength(pdfBytes.length);

      log.info("✅ PDF generado y email programado para sesión {}", sesionId);

      return ResponseEntity.ok()
          .headers(headers)
          .body(pdfBytes);

    } catch (Exception e) {
      log.error("❌ Error en proceso combinado para sesión {}: {}", sesionId, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/{id}/cerrar")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<ApiResponse<CierreCajaResponse>> cerrarSesion(
      @PathVariable Long id,
      @Valid @RequestBody CerrarSesionRequest request,
      HttpServletRequest httpRequest) {

    try {
      String token = httpRequest.getHeader("Authorization").substring(7);
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (!securityContextService.isSupervisor() &&
          !sesion.getUsuario().getId().equals(usuarioId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("No tiene permisos para cerrar esta sesión"));
      }

      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      BigDecimal diferencia = request.getMontoCierre().subtract(montoEsperado);
      BigDecimal umbral = new BigDecimal("10000"); // ₡10,000

      if (diferencia.abs().compareTo(umbral) > 0 && !securityContextService.isSupervisor()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(String.format(
                "Diferencia de ₡%.2f requiere autorización. Esperado: ₡%.2f, Cierre: ₡%.2f",
                diferencia, montoEsperado, request.getMontoCierre()
            )));
      }

      SesionCaja sesionCerrada = sesionCajaService.cerrarSesion(
          id,
          request.getMontoCierre(),
          request,
          request.getObservaciones(),
          request.getDenominaciones()
      );

      // 🆕 Usar método unificado
      CierreCajaResponse response = construirCierreCajaResponse(
          sesionCerrada,
          request,
          montoEsperado,
          id
      );

      return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada exitosamente", response));

    } catch (Exception e) {
      log.error("Error cerrando sesión: {}", e.getMessage(), e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al cerrar sesión: " + e.getMessage()));
    }
  }

  @Operation(summary = "Cerrar sesión administrativamente")
  @PostMapping("/{id}/cerrar-admin")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE', 'CAJERO')")
  public ResponseEntity<ApiResponse<CierreCajaResponse>> cerrarSesionAdmin(
      @PathVariable Long id,
      @Valid @RequestBody CerrarSesionRequest request,
      HttpServletRequest httpRequest) {

    try {
      String token = httpRequest.getHeader("Authorization").substring(7);
      Long usuarioAdminId = jwtTokenProvider.getUserIdFromToken(token);
      String usuarioAdminNombre = jwtTokenProvider.getEmailFromToken(token);

      // Verificar que la sesión existe
      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      // Verificar que está abierta
      if (sesion.getEstado() != EstadoSesion.ABIERTA) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("La sesión no está abierta"));
      }

      // Agregar nota de cierre administrativo
      String observacionesAdmin = request.getObservaciones() +
          " | Cerrado por admin: " + usuarioAdminNombre + " (ID: " + usuarioAdminId + ")";

      CerrarSesionRequest requestAdmin = CerrarSesionRequest.builder()
          .montoCierre(request.getMontoCierre())
          .montoRetirado(request.getMontoRetirado())
          .fondoCaja(request.getFondoCaja())
          .observaciones(observacionesAdmin)
          .totalEfectivo(request.getTotalEfectivo())
          .totalTarjeta(request.getTotalTarjeta())
          .totalTransferencia(request.getTotalTransferencia())
          .totalSinpe(request.getTotalSinpe())
          .denominaciones(request.getDenominaciones())
          .build();

      // Obtener monto esperado antes de cerrar
      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);

      // Cerrar sesión (sin validación de umbral para admin)
      SesionCaja sesionCerrada = sesionCajaService.cerrarSesionAdmin(
          id,
          request.getMontoCierre(),
          observacionesAdmin
      );

      // 🆕 Usar método unificado
      CierreCajaResponse response = construirCierreCajaResponse(
          sesionCerrada,
          requestAdmin,
          montoEsperado,
          id
      );

      // Log de auditoría
      log.info("Cierre administrativo de sesión {} por usuario {} - Diferencia: {}",
          id, usuarioAdminId, response.getDiferencia());

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesión cerrada administrativamente",
          response
      ));

    } catch (Exception e) {
      log.error("Error en cierre administrativo: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al cerrar sesión: " + e.getMessage()));
    }
  }

  /**
   * 🆕 Obtener el fondo de caja de la última sesión cerrada de una terminal
   * Útil para sugerir el montoInicial al abrir nueva sesión
   */
  @Operation(
      summary = "Obtener fondo de caja de última sesión cerrada",
      description = "Retorna el fondoCaja de la última sesión cerrada de una terminal específica. " +
          "Útil para pre-llenar el montoInicial al abrir nueva sesión."
  )
  @GetMapping("/terminal/{terminalId}/ultimo-fondo-caja")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<FondoCajaResponse>> obtenerUltimoFondoCaja(
      @PathVariable Long terminalId) {

    try {
      log.info("Consultando último fondo de caja para terminal: {}", terminalId);

      Optional<SesionCaja> ultimaSesion = sesionCajaService.buscarUltimaSesionCerrada(terminalId);

      if (ultimaSesion.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.ok(
            "No hay sesiones cerradas previas para esta terminal",
            FondoCajaResponse.builder()
                .terminalId(terminalId)
                .fondoCaja(BigDecimal.ZERO)
                .mensaje("Sin sesiones previas. Se sugiere iniciar con ₡0")
                .build()
        ));
      }

      SesionCaja sesion = ultimaSesion.get();

      FondoCajaResponse response = FondoCajaResponse.builder()
          .terminalId(terminalId)
          .terminalNombre(sesion.getTerminal().getNombre())
          .fondoCaja(sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO)
          .ultimaSesionId(sesion.getId())
          .fechaUltimaSesion(sesion.getFechaHoraCierre())
          .mensaje(String.format(
              "Fondo disponible: ₡%.2f de la sesión del %s",
              sesion.getFondoCaja(),
              sesion.getFechaHoraCierre().toLocalDate()
          ))
          .build();

      log.info("Fondo de caja encontrado: ₡{} para terminal {}",
          response.getFondoCaja(), terminalId);

      return ResponseEntity.ok(ApiResponse.ok(
          "Fondo de caja obtenido exitosamente",
          response
      ));

    } catch (Exception e) {
      log.error("Error obteniendo fondo de caja para terminal {}: {}", terminalId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener fondo de caja: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener sesión activa del usuario")
  @GetMapping("/mi-sesion-activa")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> obtenerMiSesionActiva(
      HttpServletRequest request) {

    Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(request));

    return sesionCajaService.buscarSesionActiva(usuarioId)
        .map(sesion -> ResponseEntity.ok(ApiResponse.ok(construirResponse(sesion))))
        .orElse(ResponseEntity.ok(ApiResponse.error("No tienes sesión activa")));
  }

  @Operation(summary = "Listar sesiones por fecha")
  @GetMapping("/por-fecha")
  @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<List<SesionCajaListResponse>>> listarPorFecha(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @RequestParam(required = false) Long terminalId) {

    List<SesionCaja> sesiones;

    if (terminalId != null) {
      sesiones = sesionCajaService.listarPorTerminalYFecha(terminalId, fecha);
    } else {
      sesiones = sesionCajaService.listarPorFecha(fecha);
    }

    List<SesionCajaListResponse> response = sesiones.stream()
        .map(this::construirListResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(summary = "Obtener resumen de caja del día")
  @GetMapping("/resumen-dia")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<ResumenCajaDiaResponse>> obtenerResumenDia(
      @RequestParam(required = false) Long sucursalId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

    if (fecha == null) {
      fecha = LocalDate.now();
    }

    // TODO: Implementar lógica de resumen
    ResumenCajaDiaResponse resumen = ResumenCajaDiaResponse.builder()
        .fecha(fecha)
        .totalSesiones(0)
        .sesionesAbiertas(0)
        .sesionesCerradas(0)
        .montoTotalVentas(null)
        .montoTotalEfectivo(null)
        .montoTotalTarjeta(null)
        .build();

    return ResponseEntity.ok(ApiResponse.ok(resumen));
  }

  @GetMapping("/{id}/cierre/recibo")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<byte[]> imprimirCierre(@PathVariable Long id) throws Exception {
    SesionCaja s = sesionCajaService.buscarPorId(id)
        .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

    List<SesionCajaDenominacion> den = sesionCajaDenominacionRepository.findBySesionCajaId(id);

    Map<String,Object> params = new HashMap<>();
    params.put("SESION_ID", s.getId());
    params.put("FECHA_APERTURA", s.getFechaHoraApertura());
    params.put("FECHA_CIERRE", s.getFechaHoraCierre());
    params.put("MONTO_INICIAL", s.getMontoInicial());
    params.put("MONTO_ESPERADO", sesionCajaService.calcularMontoEsperado(s));
    params.put("MONTO_CIERRE", s.getMontoCierre());
    params.put("DIFERENCIA", s.getMontoCierre().subtract((BigDecimal)params.get("MONTO_ESPERADO")));
    params.put("TOTAL_EFECTIVO", s.getTotalEfectivo());
    params.put("TOTAL_TARJETA", s.getTotalTarjeta());
    params.put("TOTAL_TRANSFERENCIA", s.getTotalTransferencia());
    params.put("OBSERVACIONES", s.getObservacionesCierre());

    // datasource para el subreporte/tabla de denominaciones
    JRDataSource dataSource = new JRBeanCollectionDataSource(den);

    JasperPrint jp = JasperFillManager.fillReport(
        this.getClass().getResourceAsStream("/reports/cierre_caja.jasper"),
        params,
        dataSource
    );
    byte[] pdf = JasperExportManager.exportReportToPdf(jp);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("inline", "cierre_caja_" + id + ".pdf");

    return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
  }

  @Operation(summary = "Obtener resumen detallado de sesión")
  @GetMapping("/{id}/resumen-detallado")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<ResumenCajaDetalladoDTO>> obtenerResumenDetallado(
      @PathVariable Long id,
      HttpServletRequest request) {

    try {
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(request));
      ResumenCajaDetalladoDTO resumen = sesionCajaService.obtenerResumenDetallado(id);

      return ResponseEntity.ok(ApiResponse.ok(
          "Resumen obtenido exitosamente",
          resumen
      ));

    } catch (Exception e) {
      log.error("Error obteniendo resumen detallado: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener resumen: " + e.getMessage()));
    }
  }

  @Operation(summary = "Registrar vale de caja")
  @PostMapping("/{id}/vale")
  @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<MovimientoCajaDTO>> registrarVale(
      @PathVariable Long id,
      @Valid @RequestBody RegistrarValeRequest request,
      HttpServletRequest httpRequest) {

    try {
      MovimientoCaja vale = movimientoCajaService.registrarVale(
          id,
          request.getMonto(),
          request.getConcepto()
      );

      MovimientoCajaDTO response = MovimientoCajaDTO.builder()
          .id(vale.getId())
          .tipoMovimiento(vale.getTipoMovimiento().name())
          .monto(vale.getMonto())
          .concepto(vale.getConcepto())
          .fechaHora(vale.getFechaHora())
          .autorizadoPor(vale.getAutorizadoPorId())
          .build();

      return ResponseEntity.ok(ApiResponse.ok(
          "Vale registrado exitosamente",
          response
      ));

    } catch (Exception e) {
      log.error("Error registrando vale: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al registrar vale: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener movimientos de caja")
  @GetMapping("/{id}/movimientos")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<MovimientoCajaDTO>>> obtenerMovimientos(
      @PathVariable Long id) {

    try {
      List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorSesion(id);

      List<MovimientoCajaDTO> response = movimientos.stream()
          .map(m -> MovimientoCajaDTO.builder()
              .id(m.getId())
              .tipoMovimiento(m.getTipoMovimiento().name())
              .monto(m.getMonto())
              .concepto(m.getConcepto())
              .fechaHora(m.getFechaHora())
              .observaciones(m.getObservaciones())
              .build())
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.ok(response));

    } catch (Exception e) {
      log.error("Error obteniendo movimientos: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener movimientos: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener todas las sesiones (Admin)")
  @GetMapping("/todas")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<SesionCajaResponse>>> obtenerTodasLasSesiones() {
    try {
      List<SesionCaja> sesiones = sesionCajaService.buscarTodas();

      List<SesionCajaResponse> response = sesiones.stream()
          .map(this::construirResponse)
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesiones obtenidas exitosamente",
          response
      ));
    } catch (Exception e) {
      log.error("Error obteniendo todas las sesiones: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener sesiones: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener sesiones por estado (Admin)")
  @GetMapping("/estado/{estado}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<SesionCajaResponse>>> obtenerSesionesPorEstado(
      @PathVariable String estado) {

    try {
      EstadoSesion estadoEnum = EstadoSesion.valueOf(estado.toUpperCase());
      List<SesionCaja> sesiones = sesionCajaService.buscarPorEstado(estadoEnum);

      List<SesionCajaResponse> response = sesiones.stream()
          .map(this::construirResponse)
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesiones " + estado + " obtenidas",
          response
      ));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Estado inválido: " + estado));
    } catch (Exception e) {
      log.error("Error obteniendo sesiones por estado: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @GetMapping("/{id}/cierre/recibo-ticket")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<byte[]> imprimirCierreTicket(@PathVariable Long id,
      HttpServletRequest request) {

    try {
      String token = request.getHeader("Authorization").substring(7);
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      var sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (sesion.getEstado().name().equals("ABIERTA")) {
        throw new RuntimeException("La sesión debe estar cerrada para generar el reporte");
      }

      if (!securityContextService.isSupervisor() &&
          !sesion.getUsuario().getId().equals(usuarioId)) {
        throw new RuntimeException("No tiene permisos para ver este reporte");
      }

      var denoms = sesionCajaDenominacionRepository.findBySesionCajaId(id);
      ResumenCajaDetalladoDTO resumen = sesionCajaService.obtenerResumenDetallado(id);

      StringBuilder plataformasTexto = new StringBuilder();
      if (resumen.getVentasPlataformas() != null && !resumen.getVentasPlataformas().isEmpty()) {
        plataformasTexto.append("\n--- PLATAFORMAS DIGITALES ---\n");
        BigDecimal totalPlat = BigDecimal.ZERO;
        for (ResumenCajaDetalladoDTO.VentaPlataformaDTO plat : resumen.getVentasPlataformas()) {
          plataformasTexto.append(String.format("%-15s ₡%,10.2f\n",
              "[" + plat.getPlataformaCodigo() + "] " + plat.getPlataformaNombre(),
              plat.getTotalVentas()));
          plataformasTexto.append(String.format("  (%d pedidos)\n", plat.getCantidadTransacciones()));
          totalPlat = totalPlat.add(plat.getTotalVentas());
        }
        plataformasTexto.append(String.format("Total Plataformas: ₡%,10.2f\n", totalPlat));
      }

      var params = new java.util.HashMap<String,Object>();

      Empresa empresa = sesion.getTerminal().getSucursal().getEmpresa();
      params.put("EMPRESA_NOMBRE", empresa.getNombreComercial() != null ?
          empresa.getNombreComercial() : empresa.getNombreRazonSocial());
      params.put("EMPRESA_CEDULA", empresa.getIdentificacion());

      params.put("SESION_ID", sesion.getId());
      params.put("USUARIO", sesion.getUsuario() != null ?
          sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "");
      params.put("FECHA_APERTURA", sesion.getFechaHoraApertura());
      params.put("FECHA_CIERRE", sesion.getFechaHoraCierre());

      params.put("MONTO_INICIAL", sesion.getMontoInicial());
      var esperado = sesionCajaService.calcularMontoEsperado(sesion);
      params.put("MONTO_ESPERADO", esperado);
      params.put("MONTO_CIERRE", sesion.getMontoCierre());
      params.put("DIFERENCIA", sesion.getMontoCierre().subtract(esperado));

      params.put("TOTAL_VENTAS", sesion.getTotalVentas());
      params.put("TOTAL_DEVOLUCIONES", sesion.getTotalDevoluciones());
      params.put("TOTAL_VALES", movimientoCajaService.obtenerTotalVales(id));
      params.put("TOTAL_EFECTIVO", sesion.getTotalEfectivo());
      params.put("TOTAL_TARJETA", sesion.getTotalTarjeta());
      params.put("TOTAL_TRANSFERENCIA", sesion.getTotalTransferencia());
      params.put("TOTAL_SINPE", sesion.getTotalOtros());
      params.put("OBSERVACIONES", sesion.getObservacionesCierre());
      params.put("PLATAFORMAS_TEXTO", plataformasTexto.toString());

      byte[] pdf = pdfGeneratorService.generarPdf(
          "cierre_caja_ticket",
          params,
          denoms
      );

      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline",
          "cierre_caja_ticket_" + id + ".pdf");

      return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("Error generando reporte de cierre: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(("Error: " + e.getMessage()).getBytes());
    }
  }

  @GetMapping("/sucursal/{sucursalId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<Page<SesionCajaDTO>>> listarPorSucursal(
      @PathVariable Long sucursalId,
      @PageableDefault(size = 20, sort = "fechaHoraApertura", direction = Sort.Direction.DESC) Pageable pageable) {

    log.info("Listando sesiones de caja para sucursal: {}", sucursalId);

    try {
      Page<SesionCajaDTO> sesiones = sesionCajaService.listarPorSucursal(sucursalId, pageable);

      return ResponseEntity.ok(ApiResponse.<Page<SesionCajaDTO>>builder()
          .success(true)
          .message("Sesiones de caja obtenidas exitosamente")
          .data(sesiones)
          .build());
    } catch (Exception e) {
      log.error("Error al listar sesiones de caja por sucursal", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.<Page<SesionCajaDTO>>builder()
              .success(false)
              .message("Error al obtener las sesiones de caja")
              .build());
    }
  }

  // ========== MÉTODOS PRIVADOS ==========

  /**
   * 🆕 Método privado para construir CierreCajaResponse de forma unificada
   * Evita duplicación de código entre cierre normal y cierre admin
   */
  private CierreCajaResponse construirCierreCajaResponse(
      SesionCaja sesionCerrada,
      CerrarSesionRequest request,
      BigDecimal montoEsperado,
      Long sesionId) {

    BigDecimal diferencia = sesionCerrada.getMontoCierre().subtract(montoEsperado);

    return CierreCajaResponse.builder()
        .sesionId(sesionCerrada.getId())
        .fechaApertura(sesionCerrada.getFechaHoraApertura())
        .fechaCierre(sesionCerrada.getFechaHoraCierre())
        .montoInicial(sesionCerrada.getMontoInicial())
        .totalVentas(sesionCerrada.getTotalVentas())
        .totalDevoluciones(sesionCerrada.getTotalDevoluciones())
        .totalVales(movimientoCajaService.obtenerTotalVales(sesionId))
        .montoEsperado(montoEsperado)
        .montoCierre(sesionCerrada.getMontoCierre())
        // 🆕 NUEVOS CAMPOS
        .montoRetirado(sesionCerrada.getMontoRetirado())
        .fondoCaja(sesionCerrada.getFondoCaja())
        .diferencia(diferencia)
        .cantidadFacturas(sesionCerrada.getCantidadFacturas())
        .cantidadTiquetes(sesionCerrada.getCantidadTiquetes())
        .cantidadNotasCredito(sesionCerrada.getCantidadNotasCredito())
        .totalEfectivo(sesionCerrada.getTotalEfectivo())
        .totalTarjeta(sesionCerrada.getTotalTarjeta())
        .totalTransferencia(sesionCerrada.getTotalTransferencia())
        .observaciones(sesionCerrada.getObservacionesCierre())
        .denominaciones(request.getDenominaciones())
        .build();
  }

  private SesionCajaResponse construirResponse(SesionCaja sesion) {
    return SesionCajaResponse.builder()
        .id(sesion.getId())
        .terminalId(sesion.getTerminal().getId())
        .terminalNombre(sesion.getTerminal().getNombre())
        .sucursalNombre(sesion.getTerminal().getSucursal().getNombre())
        .usuarioNombre(sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos())
        .fechaApertura(sesion.getFechaHoraApertura())
        .montoInicial(sesion.getMontoInicial())
        .totalVentas(sesion.getTotalVentas())
        .estado(sesion.getEstado().name())
        .cantidadFacturas(sesion.getCantidadFacturas())
        // 🆕 NUEVOS CAMPOS (solo si la sesión está cerrada)
        .montoRetirado(sesion.getMontoRetirado())
        .fondoCaja(sesion.getFondoCaja())
        .build();
  }

  private SesionCajaListResponse construirListResponse(SesionCaja sesion) {
    return SesionCajaListResponse.builder()
        .id(sesion.getId())
        .terminal(sesion.getTerminal().getNombre())
        .cajero(sesion.getUsuario().getNombre())
        .fechaApertura(sesion.getFechaHoraApertura())
        .fechaCierre(sesion.getFechaHoraCierre())
        .estado(sesion.getEstado().name())
        .totalVentas(sesion.getTotalVentas())
        .diferencia(sesion.getDiferenciaCierre())
        .build();
  }

  private String getToken(HttpServletRequest httpRequest) {
    String token = httpRequest.getHeader("Authorization");
    token = token.substring(7);
    return token;
  }
}