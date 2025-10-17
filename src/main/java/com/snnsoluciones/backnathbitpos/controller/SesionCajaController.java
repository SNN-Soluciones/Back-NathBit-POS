package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.*;
import com.snnsoluciones.backnathbitpos.dto.sesiones.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.RegistrarValeRequest;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.SesionCajaDTO;
import com.snnsoluciones.backnathbitpos.entity.CierreDatafono;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.repository.CierreDatafonoRepository;
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
import java.util.ArrayList;
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
  private final CierreDatafonoRepository cierreDatafonoRepository;

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

    log.info("📄 Generando cierre de caja mejorado para sesión: {}", id);

    try {
      // 1️⃣ OBTENER LA SESIÓN
      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      // 2️⃣ VALIDAR QUE ESTÉ CERRADA
      if (sesion.getEstado().name().equals("ABIERTA")) {
        throw new RuntimeException("La sesión debe estar cerrada para generar el reporte");
      }

      // 3️⃣ OBTENER TODAS LAS ENTIDADES RELACIONADAS
      Empresa empresa = sesion.getTerminal().getSucursal().getEmpresa();
      List<SesionCajaDenominacion> denominaciones = sesionCajaDenominacionRepository.findBySesionCajaId(id);

      // 🆕 NUEVOS DATOS A BUSCAR
      List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorSesion(id);
      List<CierreDatafono> datafonos = cierreDatafonoRepository.findBySesionCajaId(id);

      Map<String, Integer> conteoDocumentos = sesionCajaService.contarDocumentosPorTipo(id);

      // 4️⃣ CALCULAR TOTALES DE MOVIMIENTOS
      BigDecimal totalEntradas = BigDecimal.ZERO;
      BigDecimal totalSalidas = BigDecimal.ZERO;
      BigDecimal totalVales = BigDecimal.ZERO;
      BigDecimal totalArqueos = BigDecimal.ZERO;
      BigDecimal totalPagosProveedor = BigDecimal.ZERO;

      for (MovimientoCaja mov : movimientos) {
        if (mov.getTipoMovimiento().name().startsWith("ENTRADA")) {
          totalEntradas = totalEntradas.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_VALE")) {
          totalVales = totalVales.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_ARQUEO")) {
          totalArqueos = totalArqueos.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_PAGO_PROVEEDOR")) {
          totalPagosProveedor = totalPagosProveedor.add(mov.getMonto());
        }

        if (mov.getTipoMovimiento().name().startsWith("SALIDA")) {
          totalSalidas = totalSalidas.add(mov.getMonto());
        }
      }

      // 5️⃣ PREPARAR PARÁMETROS PARA EL REPORTE
      Map<String, Object> params = new HashMap<>();

      // DATOS EMPRESA
      params.put("EMPRESA_NOMBRE", empresa.getNombreComercial() != null ?
          empresa.getNombreComercial() : empresa.getNombreRazonSocial());
      params.put("EMPRESA_CEDULA", empresa.getIdentificacion());

      // DATOS SESIÓN
      params.put("SESION_ID", sesion.getId());
      params.put("USUARIO", sesion.getUsuario() != null ?
          sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "");
      params.put("TERMINAL", sesion.getTerminal().getNombre());
      params.put("SUCURSAL", sesion.getTerminal().getSucursal().getNombre());
      params.put("FECHA_APERTURA", sesion.getFechaHoraApertura());
      params.put("FECHA_CIERRE", sesion.getFechaHoraCierre());

      // MONTOS BÁSICOS
      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      params.put("MONTO_INICIAL", sesion.getMontoInicial());
      params.put("MONTO_ESPERADO", montoEsperado);
      params.put("MONTO_CIERRE", sesion.getMontoCierre());
      params.put("DIFERENCIA", sesion.getMontoCierre().subtract(montoEsperado));

      // TOTALES DE VENTAS Y MEDIOS DE PAGO
      params.put("TOTAL_VENTAS", sesion.getTotalVentas());
      params.put("TOTAL_DEVOLUCIONES", sesion.getTotalDevoluciones());
      params.put("TOTAL_EFECTIVO", sesion.getTotalEfectivo());
      params.put("TOTAL_TARJETA", sesion.getTotalTarjeta());
      params.put("TOTAL_TRANSFERENCIA", sesion.getTotalTransferencia());
      params.put("TOTAL_SINPE", sesion.getTotalOtros());

      // 🆕 MOVIMIENTOS DISCRIMINADOS
      params.put("TOTAL_ENTRADAS", totalEntradas);
      params.put("TOTAL_SALIDAS", totalSalidas);
      params.put("TOTAL_VALES", totalVales);
      params.put("TOTAL_ARQUEOS", totalArqueos);
      params.put("TOTAL_PAGOS_PROVEEDOR", totalPagosProveedor);

      // 🆕 CONTEO DE DOCUMENTOS
      params.put("CANT_FACTURAS_ELECTRONICAS", conteoDocumentos.getOrDefault("FACTURA_ELECTRONICA", 0));
      params.put("CANT_TIQUETES_ELECTRONICOS", conteoDocumentos.getOrDefault("TIQUETE_ELECTRONICO", 0));
      params.put("CANT_FACTURAS_INTERNAS", conteoDocumentos.getOrDefault("FACTURA_INTERNA", 0));
      params.put("CANT_TIQUETES_INTERNOS", conteoDocumentos.getOrDefault("TIQUETE_INTERNO", 0));
      params.put("CANT_NOTAS_CREDITO", conteoDocumentos.getOrDefault("NOTA_CREDITO", 0));
      params.put("TOTAL_DOCUMENTOS", conteoDocumentos.values().stream().mapToInt(Integer::intValue).sum());

      // 🆕 DISTRIBUCIÓN DE EFECTIVO
      params.put("MONTO_RETIRADO", sesion.getMontoRetirado() != null ? sesion.getMontoRetirado() : BigDecimal.ZERO);
      params.put("FONDO_CAJA", sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO);

      // OBSERVACIONES
      params.put("OBSERVACIONES", sesion.getObservacionesCierre() != null ?
          sesion.getObservacionesCierre() : "Sin observaciones");

      // 🆕 DATASOURCES PARA SUBREPORTES
      params.put("MOVIMIENTOS_DS", new JRBeanCollectionDataSource(movimientos));
      params.put("DATAFONOS_DS", new JRBeanCollectionDataSource(datafonos));
      params.put("DENOMINACIONES_DS", new JRBeanCollectionDataSource(denominaciones));

      // 6️⃣ GENERAR PDF
      byte[] pdf = pdfGeneratorService.generarPdf(
          "cierre_caja_ticket",
          params,
          new ArrayList<>()  // ✅ Lista vacía porque los datos van en params
      );

      // 7️⃣ PREPARAR RESPUESTA
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline",
          "cierre_caja_ticket_" + id + ".pdf");

      log.info("✅ Cierre de caja generado exitosamente para sesión {}", id);

      return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("❌ Error generando cierre de caja para sesión {}: {}", id, e.getMessage(), e);
      throw new RuntimeException("Error generando reporte: " + e.getMessage());
    }
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

  @GetMapping("/{id}/cierre/html")
  public ResponseEntity<String> obtenerHtmlCierre(@PathVariable Long id) {
    OpcionesImpresionCierreDTO opciones = new OpcionesImpresionCierreDTO();
    opciones.setIncluirMovimientos(true);
    opciones.setIncluirFacturas(true);

    String html = sesionCajaService.generarHtmlCierre(id, opciones);

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html);
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

    log.info("📄 Generando cierre de caja mejorado para sesión: {}", id);

    try {
      // 1️⃣ VALIDACIONES DE SEGURIDAD
      String token = request.getHeader("Authorization").substring(7);
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (sesion.getEstado().name().equals("ABIERTA")) {
        throw new RuntimeException("La sesión debe estar cerrada para generar el reporte");
      }

      if (!securityContextService.isSupervisor() &&
          !sesion.getUsuario().getId().equals(usuarioId)) {
        throw new RuntimeException("No tiene permisos para ver este reporte");
      }

      // 2️⃣ OBTENER TODAS LAS ENTIDADES RELACIONADAS
      Empresa empresa = sesion.getTerminal().getSucursal().getEmpresa();
      List<SesionCajaDenominacion> denominaciones = sesionCajaDenominacionRepository.findBySesionCajaId(id);
      List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorSesion(id);
      List<CierreDatafono> datafonos = cierreDatafonoRepository.findBySesionCajaId(id);

      // Conteo de documentos
      Map<String, Integer> conteoDocumentos = sesionCajaService.contarDocumentosPorTipo(id);

      // 3️⃣ CALCULAR TOTALES DE MOVIMIENTOS
      BigDecimal totalEntradas = BigDecimal.ZERO;
      BigDecimal totalSalidas = BigDecimal.ZERO;
      BigDecimal totalVales = BigDecimal.ZERO;
      BigDecimal totalArqueos = BigDecimal.ZERO;
      BigDecimal totalPagosProveedor = BigDecimal.ZERO;

      for (MovimientoCaja mov : movimientos) {
        if (mov.getTipoMovimiento().name().startsWith("ENTRADA")) {
          totalEntradas = totalEntradas.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_VALE")) {
          totalVales = totalVales.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_ARQUEO")) {
          totalArqueos = totalArqueos.add(mov.getMonto());
        } else if (mov.getTipoMovimiento().name().equals("SALIDA_PAGO_PROVEEDOR")) {
          totalPagosProveedor = totalPagosProveedor.add(mov.getMonto());
        }

        if (mov.getTipoMovimiento().name().startsWith("SALIDA")) {
          totalSalidas = totalSalidas.add(mov.getMonto());
        }
      }

      // 4️⃣ PREPARAR PARÁMETROS PARA EL REPORTE
      Map<String, Object> params = new HashMap<>();

      // DATOS EMPRESA
      params.put("EMPRESA_NOMBRE", empresa.getNombreComercial() != null ?
          empresa.getNombreComercial() : empresa.getNombreRazonSocial());
      params.put("EMPRESA_CEDULA", empresa.getIdentificacion());

      // DATOS SESIÓN
      params.put("SESION_ID", sesion.getId());
      params.put("USUARIO", sesion.getUsuario() != null ?
          sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "");
      params.put("TERMINAL", sesion.getTerminal().getNombre());
      params.put("SUCURSAL", sesion.getTerminal().getSucursal().getNombre());
      params.put("FECHA_APERTURA", sesion.getFechaHoraApertura());
      params.put("FECHA_CIERRE", sesion.getFechaHoraCierre());

      // MONTOS BÁSICOS
      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      params.put("MONTO_INICIAL", sesion.getMontoInicial());
      params.put("MONTO_ESPERADO", montoEsperado);
      params.put("MONTO_CIERRE", sesion.getMontoCierre());
      params.put("DIFERENCIA", sesion.getMontoCierre().subtract(montoEsperado));

      // TOTALES DE VENTAS Y MEDIOS DE PAGO
      params.put("TOTAL_VENTAS", sesion.getTotalVentas());
      params.put("TOTAL_DEVOLUCIONES", sesion.getTotalDevoluciones());
      params.put("TOTAL_EFECTIVO", sesion.getTotalEfectivo());
      params.put("TOTAL_TARJETA", sesion.getTotalTarjeta());
      params.put("TOTAL_TRANSFERENCIA", sesion.getTotalTransferencia());
      params.put("TOTAL_SINPE", sesion.getTotalOtros());

      // 🆕 MOVIMIENTOS DISCRIMINADOS
      params.put("TOTAL_ENTRADAS", totalEntradas);
      params.put("TOTAL_SALIDAS", totalSalidas);
      params.put("TOTAL_VALES", totalVales);
      params.put("TOTAL_ARQUEOS", totalArqueos);
      params.put("TOTAL_PAGOS_PROVEEDOR", totalPagosProveedor);

      // 🆕 CONTEO DE DOCUMENTOS
      params.put("CANT_FACTURAS_ELECTRONICAS", conteoDocumentos.getOrDefault("FACTURA_ELECTRONICA", 0));
      params.put("CANT_TIQUETES_ELECTRONICOS", conteoDocumentos.getOrDefault("TIQUETE_ELECTRONICO", 0));
      params.put("CANT_FACTURAS_INTERNAS", conteoDocumentos.getOrDefault("FACTURA_INTERNA", 0));
      params.put("CANT_TIQUETES_INTERNOS", conteoDocumentos.getOrDefault("TIQUETE_INTERNO", 0));
      params.put("CANT_NOTAS_CREDITO", conteoDocumentos.getOrDefault("NOTA_CREDITO", 0));
      params.put("TOTAL_DOCUMENTOS", conteoDocumentos.values().stream().mapToInt(Integer::intValue).sum());

      // 🆕 DISTRIBUCIÓN DE EFECTIVO
      params.put("MONTO_RETIRADO", sesion.getMontoRetirado() != null ? sesion.getMontoRetirado() : BigDecimal.ZERO);
      params.put("FONDO_CAJA", sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO);

      // OBSERVACIONES
      params.put("OBSERVACIONES", sesion.getObservacionesCierre() != null ?
          sesion.getObservacionesCierre() : "Sin observaciones");

      // 🆕 DATASOURCES PARA SUBREPORTES
      params.put("MOVIMIENTOS_DS", new JRBeanCollectionDataSource(movimientos));
      params.put("DATAFONOS_DS", new JRBeanCollectionDataSource(datafonos));
      params.put("DENOMINACIONES_DS", new JRBeanCollectionDataSource(denominaciones));

      // 5️⃣ GENERAR PDF
      byte[] pdf = pdfGeneratorService.generarPdf(
          "cierre_caja_ticket",
          params,
          new ArrayList<>()  // ✅ Lista vacía - los datos van en params
      );

      // 6️⃣ PREPARAR RESPUESTA
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline",
          "cierre_caja_ticket_" + id + ".pdf");

      log.info("✅ Cierre de caja generado exitosamente para sesión {}", id);

      return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("❌ Error generando cierre de caja para sesión {}: {}", id, e.getMessage(), e);
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