package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.*;
import com.snnsoluciones.backnathbitpos.dto.sesiones.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.RegistrarValeRequest;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.SesionCajaDTO;
import com.snnsoluciones.backnathbitpos.entity.CierreDatafono;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCajaDenominacion;
import com.snnsoluciones.backnathbitpos.entity.SesionCajaUsuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CierreDatafonoRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaInternaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaDenominacionRepository;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaUsuarioRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
  private final SesionCajaUsuarioRepository sesionCajaUsuarioRepository;
  private final FacturaRepository facturaRepository;
  private final FacturaInternaRepository facturaInternaRepository;

  // =========================================================================
  // APERTURA
  // =========================================================================

  @Operation(summary = "Abrir sesión de caja")
  @PostMapping("/abrir")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> abrirSesion(
      @Valid @RequestBody AbrirSesionRequest request,
      HttpServletRequest httpRequest) {

    try {
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(httpRequest));

      // ✅ Fix: verificar turno activo (no dueño de sesión — eso no aplica en SHARED)
      Optional<SesionCajaUsuario> turnoActivo = sesionCajaUsuarioRepository
          .findTurnoActivoUsuario(usuarioId);

      if (turnoActivo.isPresent()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "Ya tenés un turno activo en la terminal: "
                    + turnoActivo.get().getSesionCaja().getTerminal().getNombre()
                    + ". Cerrá ese turno antes de abrir otra sesión."));
      }

      // ✅ Fix: si la terminal ya tiene sesión SHARED activa → redirigir a unirse
      if (sesionCajaService.terminalTieneSesionAbierta(request.getTerminalId())) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "La terminal ya tiene una sesión abierta. Usá 'Unirse a turno' en lugar de abrir una nueva."));
      }

      SesionCaja sesion = sesionCajaService.abrirSesion(
          usuarioId,
          request.getTerminalId(),
          request.getMontoInicial()
      );

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesión de caja abierta exitosamente",
          construirResponse(sesion)
      ));

    } catch (Exception e) {
      log.error("Error abriendo sesión: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al abrir sesión: " + e.getMessage()));
    }
  }

  // =========================================================================
  // TURNOS SHARED
  // =========================================================================

  @Operation(summary = "Obtener turnos de una sesión compartida")
  @GetMapping("/{sesionCajaId}/turnos")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<SesionCajaUsuarioDTO>>> obtenerTurnos(
      @PathVariable Long sesionCajaId) {
    try {
      List<SesionCajaUsuario> turnos = sesionCajaUsuarioRepository
          .findBySesionCajaId(sesionCajaId);

      return ResponseEntity.ok(ApiResponse.ok("Turnos obtenidos exitosamente",
          turnos.stream().map(this::mapTurnoDTO).collect(Collectors.toList())));
    } catch (Exception e) {
      log.error("Error obteniendo turnos de sesión {}: {}", sesionCajaId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener turnos: " + e.getMessage()));
    }
  }

  @Operation(summary = "Unirse a turno en sesión compartida",
      description = "El cajero se une con su propio JWT. "
          + "ADMIN/JEFE_CAJAS puede pasar usuarioId en el body para unir a otro cajero.")
  @PostMapping("/{sesionCajaId}/turnos/unirse")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaUsuarioDTO>> unirseATurno(
      @PathVariable Long sesionCajaId,
      @RequestBody(required = false) Map<String, Long> body,
      HttpServletRequest httpRequest) {
    try {
      Long usuarioIdJwt = jwtTokenProvider.getUserIdFromToken(getToken(httpRequest));

      // Si viene usuarioId en el body Y el caller es supervisor → unir a ese usuario
      Long usuarioIdFinal = usuarioIdJwt;
      if (body != null && body.containsKey("usuarioId") && securityContextService.isSupervisor()) {
        usuarioIdFinal = body.get("usuarioId");
        log.info("Admin {} une al usuario {} a sesión {}", usuarioIdJwt, usuarioIdFinal, sesionCajaId);
      }

      SesionCajaUsuario turno = sesionCajaService.unirseATurno(usuarioIdFinal, sesionCajaId);

      return ResponseEntity.ok(ApiResponse.ok("Turno iniciado exitosamente", mapTurnoDTO(turno)));

    } catch (Exception e) {
      log.error("Error uniéndose a turno: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al unirse al turno: " + e.getMessage()));
    }
  }

  @Operation(summary = "Cerrar turno de usuario en sesión compartida")
  @PostMapping("/turnos/{turnoId}/cerrar")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<CerrarTurnoResponse>> cerrarTurno(
      @PathVariable Long turnoId,
      @Valid @RequestBody CerrarTurnoRequest request,
      HttpServletRequest httpRequest) {
    try {
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(httpRequest));

      SesionCajaUsuario turno = sesionCajaUsuarioRepository.findById(turnoId)
          .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

      // Solo el dueño del turno, JEFE_CAJAS o ADMIN pueden cerrar
      boolean esDueno = turno.getUsuario().getId().equals(usuarioId);
      boolean esSupervisor = securityContextService.isSupervisor();

      if (!esDueno && !esSupervisor) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("No tenés permisos para cerrar el turno de otro cajero"));
      }

      CerrarTurnoResponse response = sesionCajaService.cerrarTurno(turnoId, request);

      return ResponseEntity.ok(ApiResponse.ok("Turno cerrado exitosamente", response));

    } catch (Exception e) {
      log.error("Error cerrando turno {}: {}", turnoId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al cerrar turno: " + e.getMessage()));
    }
  }

  @Operation(summary = "Confirmar cierre definitivo de sesión SHARED",
      description = "Se llama después de cerrarTurno cuando esSesionCerrada=true. "
          + "Cierra la sesión maestra solo si todos los turnos están cerrados.")
  @PostMapping("/{sesionCajaId}/confirmar-cierre")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> confirmarCierreSesion(
      @PathVariable Long sesionCajaId,
      HttpServletRequest httpRequest) {
    try {
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(httpRequest));

      SesionCaja sesion = sesionCajaService.confirmarCierreSesion(sesionCajaId, usuarioId);

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesión cerrada definitivamente",
          construirResponse(sesion)
      ));

    } catch (Exception e) {
      log.error("Error confirmando cierre de sesión {}: {}", sesionCajaId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al confirmar cierre: " + e.getMessage()));
    }
  }

  // =========================================================================
  // CONSULTAS DE ESTADO
  // =========================================================================

  @Operation(summary = "Obtener mi turno activo")
  @GetMapping("/mi-turno-activo")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaUsuarioDTO>> obtenerMiTurnoActivo(
      HttpServletRequest request) {

    Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(request));

    return sesionCajaUsuarioRepository.findTurnoActivoUsuario(usuarioId)
        .map(turno -> ResponseEntity.ok(ApiResponse.ok(
            "Turno activo encontrado",
            mapTurnoDTO(turno)
        )))
        .orElse(ResponseEntity.ok(ApiResponse.ok("No tienes turno activo", null)));
  }

  @Operation(summary = "Obtener sesión activa por terminal")
  @GetMapping("/terminal/{terminalId}/activa")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> obtenerSesionActivaPorTerminal(
      @PathVariable Long terminalId) {

    return sesionCajaService.obtenerSesionActivaPorTerminal(terminalId)
        .map(sesion -> ResponseEntity.ok(ApiResponse.ok(
            "Sesión activa encontrada",
            construirResponse(sesion)
        )))
        .orElse(ResponseEntity.ok(ApiResponse.ok("No hay sesión activa en esta terminal", null)));
  }

  @Operation(summary = "Listar todas las sesiones SHARED activas de una sucursal")
  @GetMapping("/sucursal/{sucursalId}/activas")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<SesionCajaResponse>>> listarSesionesActivasPorSucursal(
      @PathVariable Long sucursalId) {
    try {
      List<SesionCaja> sesiones = sesionCajaService
          .obtenerSesionesSharedActivasPorSucursal(sucursalId);

      // Usar findSharedActivasBySucursal directamente devuelve lista — delegamos al service
      List<SesionCajaResponse> response = sesiones.stream()
          .map(this::construirResponse)
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesiones activas encontradas: " + response.size(),
          response
      ));
    } catch (Exception e) {
      log.error("Error listando sesiones activas sucursal {}: {}", sucursalId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener sesión activa del usuario (legacy)")
  @GetMapping("/mi-sesion-activa")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<SesionCajaResponse>> obtenerMiSesionActiva(
      HttpServletRequest request) {

    Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(request));

    return sesionCajaService.buscarSesionActiva(usuarioId)
        .map(sesion -> ResponseEntity.ok(ApiResponse.ok(construirResponse(sesion))))
        .orElse(ResponseEntity.ok(ApiResponse.error("No tienes sesión activa")));
  }

  // =========================================================================
  // RESUMEN DE TURNO
  // =========================================================================

  @Transactional(readOnly = true)  // ← agregar esto
  @Operation(summary = "Resumen ligero para cierre de turno SHARED")
  @GetMapping("/turnos/{turnoId}/resumen")
  @PreAuthorize("hasAnyRole('ROOT','SUPER_ADMIN','ADMIN','JEFE_CAJAS','CAJERO')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerResumenTurno(
      @PathVariable Long turnoId) {

    return sesionCajaUsuarioRepository.findById(turnoId)
        .map(turno -> {
          SesionCaja sesion = turno.getSesionCaja();
          LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Costa_Rica"));

          BigDecimal montoEsperado = sesionCajaService
              .calcularMontoEsperadoEfectivoHasta(sesion, ahora);

          // ← Calcular desde facturas reales del turno
          List<Factura> facturas = facturaRepository.findByTurnoId(turnoId);
          List<FacturaInterna> internas = facturaInternaRepository.findByTurnoId(turnoId);

          BigDecimal ventasEfectivo      = BigDecimal.ZERO;
          BigDecimal ventasTarjeta       = BigDecimal.ZERO;
          BigDecimal ventasTransferencia = BigDecimal.ZERO;
          BigDecimal ventasOtros         = BigDecimal.ZERO;

          for (Factura f : facturas) {
            if (f.getMediosPago() != null) {
              for (var mp : f.getMediosPago()) {
                switch (obtenerMetodoPagoEstandar(mp.getMedioPago().name())) {
                  case "E"  -> ventasEfectivo      = ventasEfectivo.add(mp.getMonto());
                  case "TC" -> ventasTarjeta        = ventasTarjeta.add(mp.getMonto());
                  case "TB" -> ventasTransferencia  = ventasTransferencia.add(mp.getMonto());
                  case "S"  -> ventasOtros           = ventasOtros.add(mp.getMonto());
                }
              }
            }
          }

          for (FacturaInterna fi : internas) {
            if (fi.getMediosPago() != null) {
              for (var mp : fi.getMediosPago()) {
                switch (obtenerMetodoPagoEstandar(mp.getTipo())) {
                  case "E"  -> ventasEfectivo      = ventasEfectivo.add(mp.getMonto());
                  case "TC" -> ventasTarjeta        = ventasTarjeta.add(mp.getMonto());
                  case "TB" -> ventasTransferencia  = ventasTransferencia.add(mp.getMonto());
                  case "S"  -> ventasOtros           = ventasOtros.add(mp.getMonto());
                }
              }
            }
          }

          Map<String, Object> data = new java.util.LinkedHashMap<>();
          data.put("turnoId",              turno.getId());
          data.put("sesionCajaId",         sesion.getId());
          data.put("terminal",             sesion.getTerminal().getNombre());
          data.put("cajero",               turno.getUsuario().getNombre() + " " + turno.getUsuario().getApellidos());
          data.put("fechaApertura",        sesion.getFechaHoraApertura());
          data.put("fechaInicio",          turno.getFechaHoraInicio());
          data.put("montoInicial",         sesion.getMontoInicial());
          data.put("fondoInicioTurno",     turno.getFondoInicioTurno());
          data.put("ventasEfectivo",       ventasEfectivo);
          data.put("ventasTarjeta",        ventasTarjeta);
          data.put("ventasTransferencia",  ventasTransferencia);
          data.put("ventasOtros",          ventasOtros);
          data.put("montoEsperado",        montoEsperado);
          data.put("estado",               turno.getEstado());

          return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
              .success(true).data(data).build());
        })
        .orElse(ResponseEntity.notFound().build());
  }

  // =========================================================================
  // CIERRE DIRECTO (sesión no-SHARED o admin)
  // =========================================================================

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
      BigDecimal umbral = new BigDecimal("10000");

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

      CierreCajaResponse response = construirCierreCajaResponse(
          sesionCerrada, request, montoEsperado, id);

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

      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (sesion.getEstado() != EstadoSesion.ABIERTA) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("La sesión no está abierta"));
      }

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

      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);

      SesionCaja sesionCerrada = sesionCajaService.cerrarSesionAdmin(
          id, request.getMontoCierre(), observacionesAdmin);

      CierreCajaResponse response = construirCierreCajaResponse(
          sesionCerrada, requestAdmin, montoEsperado, id);

      log.info("Cierre administrativo de sesión {} por usuario {} - Diferencia: {}",
          id, usuarioAdminId, response.getDiferencia());

      return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada administrativamente", response));

    } catch (Exception e) {
      log.error("Error en cierre administrativo: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al cerrar sesión: " + e.getMessage()));
    }
  }

  // =========================================================================
  // REPORTES Y PDF
  // =========================================================================

  @GetMapping("/{id}/cierre/recibo")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<byte[]> imprimirCierre(@PathVariable Long id) throws Exception {

    log.info("📄 Generando cierre de caja mejorado para sesión: {}", id);

    try {
      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (sesion.getEstado().name().equals("ABIERTA")) {
        throw new RuntimeException("La sesión debe estar cerrada para generar el reporte");
      }

      Empresa empresa = sesion.getTerminal().getSucursal().getEmpresa();
      List<SesionCajaDenominacion> denominaciones = sesionCajaDenominacionRepository.findBySesionCajaId(id);
      List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorSesion(id);
      List<CierreDatafono> datafonos = cierreDatafonoRepository.findBySesionCajaId(id);
      Map<String, Integer> conteoDocumentos = sesionCajaService.contarDocumentosPorTipo(id);

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

      Map<String, Object> params = new HashMap<>();
      params.put("EMPRESA_NOMBRE", empresa.getNombreComercial() != null ?
          empresa.getNombreComercial() : empresa.getNombreRazonSocial());
      params.put("EMPRESA_CEDULA", empresa.getIdentificacion());
      params.put("SESION_ID", sesion.getId());
      params.put("USUARIO", sesion.getUsuario() != null ?
          sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "");
      params.put("TERMINAL", sesion.getTerminal().getNombre());
      params.put("SUCURSAL", sesion.getTerminal().getSucursal().getNombre());
      params.put("FECHA_APERTURA", sesion.getFechaHoraApertura());
      params.put("FECHA_CIERRE", sesion.getFechaHoraCierre());

      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      params.put("MONTO_INICIAL", sesion.getMontoInicial());
      params.put("MONTO_ESPERADO", montoEsperado);
      params.put("MONTO_CIERRE", sesion.getMontoCierre());
      params.put("DIFERENCIA", sesion.getMontoCierre().subtract(montoEsperado));
      params.put("TOTAL_VENTAS", sesion.getTotalVentas());
      params.put("TOTAL_DEVOLUCIONES", sesion.getTotalDevoluciones());
      params.put("TOTAL_EFECTIVO", sesion.getTotalEfectivo());
      params.put("TOTAL_TARJETA", sesion.getTotalTarjeta());
      params.put("TOTAL_TRANSFERENCIA", sesion.getTotalTransferencia());
      params.put("TOTAL_SINPE", sesion.getTotalOtros());
      params.put("TOTAL_ENTRADAS", totalEntradas);
      params.put("TOTAL_SALIDAS", totalSalidas);
      params.put("TOTAL_VALES", totalVales);
      params.put("TOTAL_ARQUEOS", totalArqueos);
      params.put("TOTAL_PAGOS_PROVEEDOR", totalPagosProveedor);
      params.put("CANT_FACTURAS_ELECTRONICAS", conteoDocumentos.getOrDefault("FACTURA_ELECTRONICA", 0));
      params.put("CANT_TIQUETES_ELECTRONICOS", conteoDocumentos.getOrDefault("TIQUETE_ELECTRONICO", 0));
      params.put("CANT_FACTURAS_INTERNAS", conteoDocumentos.getOrDefault("FACTURA_INTERNA", 0));
      params.put("CANT_TIQUETES_INTERNOS", conteoDocumentos.getOrDefault("TIQUETE_INTERNO", 0));
      params.put("CANT_NOTAS_CREDITO", conteoDocumentos.getOrDefault("NOTA_CREDITO", 0));
      params.put("TOTAL_DOCUMENTOS", conteoDocumentos.values().stream().mapToInt(Integer::intValue).sum());
      params.put("MONTO_RETIRADO", sesion.getMontoRetirado() != null ? sesion.getMontoRetirado() : BigDecimal.ZERO);
      params.put("FONDO_CAJA", sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO);
      params.put("OBSERVACIONES", sesion.getObservacionesCierre() != null ?
          sesion.getObservacionesCierre() : "Sin observaciones");
      params.put("MOVIMIENTOS_DS", new JRBeanCollectionDataSource(movimientos));
      params.put("DATAFONOS_DS", new JRBeanCollectionDataSource(datafonos));
      params.put("DENOMINACIONES_DS", new JRBeanCollectionDataSource(denominaciones));

      byte[] pdf = pdfGeneratorService.generarPdf("cierre_caja_ticket", params, new ArrayList<>());

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline", "cierre_caja_ticket_" + id + ".pdf");

      log.info("✅ Cierre de caja generado exitosamente para sesión {}", id);
      return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("❌ Error generando cierre de caja para sesión {}: {}", id, e.getMessage(), e);
      throw new RuntimeException("Error generando reporte: " + e.getMessage());
    }
  }

  @GetMapping("/{id}/cierre/recibo-ticket")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<byte[]> imprimirCierreTicket(@PathVariable Long id,
      HttpServletRequest request) {

    log.info("📄 Generando cierre de caja mejorado para sesión: {}", id);

    try {
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

      Empresa empresa = sesion.getTerminal().getSucursal().getEmpresa();
      List<SesionCajaDenominacion> denominaciones = sesionCajaDenominacionRepository.findBySesionCajaId(id);
      List<MovimientoCaja> movimientos = movimientoCajaService.obtenerMovimientosPorSesion(id);
      List<CierreDatafono> datafonos = cierreDatafonoRepository.findBySesionCajaId(id);
      Map<String, Integer> conteoDocumentos = sesionCajaService.contarDocumentosPorTipo(id);

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

      Map<String, Object> params = new HashMap<>();
      params.put("EMPRESA_NOMBRE", empresa.getNombreComercial() != null ?
          empresa.getNombreComercial() : empresa.getNombreRazonSocial());
      params.put("EMPRESA_CEDULA", empresa.getIdentificacion());
      params.put("SESION_ID", sesion.getId());
      params.put("USUARIO", sesion.getUsuario() != null ?
          sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : "");
      params.put("TERMINAL", sesion.getTerminal().getNombre());
      params.put("SUCURSAL", sesion.getTerminal().getSucursal().getNombre());
      params.put("FECHA_APERTURA", sesion.getFechaHoraApertura());
      params.put("FECHA_CIERRE", sesion.getFechaHoraCierre());

      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      params.put("MONTO_INICIAL", sesion.getMontoInicial());
      params.put("MONTO_ESPERADO", montoEsperado);
      params.put("MONTO_CIERRE", sesion.getMontoCierre());
      params.put("DIFERENCIA", sesion.getMontoCierre().subtract(montoEsperado));
      params.put("TOTAL_VENTAS", sesion.getTotalVentas());
      params.put("TOTAL_DEVOLUCIONES", sesion.getTotalDevoluciones());
      params.put("TOTAL_EFECTIVO", sesion.getTotalEfectivo());
      params.put("TOTAL_TARJETA", sesion.getTotalTarjeta());
      params.put("TOTAL_TRANSFERENCIA", sesion.getTotalTransferencia());
      params.put("TOTAL_SINPE", sesion.getTotalOtros());
      params.put("TOTAL_ENTRADAS", totalEntradas);
      params.put("TOTAL_SALIDAS", totalSalidas);
      params.put("TOTAL_VALES", totalVales);
      params.put("TOTAL_ARQUEOS", totalArqueos);
      params.put("TOTAL_PAGOS_PROVEEDOR", totalPagosProveedor);
      params.put("CANT_FACTURAS_ELECTRONICAS", conteoDocumentos.getOrDefault("FACTURA_ELECTRONICA", 0));
      params.put("CANT_TIQUETES_ELECTRONICOS", conteoDocumentos.getOrDefault("TIQUETE_ELECTRONICO", 0));
      params.put("CANT_FACTURAS_INTERNAS", conteoDocumentos.getOrDefault("FACTURA_INTERNA", 0));
      params.put("CANT_TIQUETES_INTERNOS", conteoDocumentos.getOrDefault("TIQUETE_INTERNO", 0));
      params.put("CANT_NOTAS_CREDITO", conteoDocumentos.getOrDefault("NOTA_CREDITO", 0));
      params.put("TOTAL_DOCUMENTOS", conteoDocumentos.values().stream().mapToInt(Integer::intValue).sum());
      params.put("MONTO_RETIRADO", sesion.getMontoRetirado() != null ? sesion.getMontoRetirado() : BigDecimal.ZERO);
      params.put("FONDO_CAJA", sesion.getFondoCaja() != null ? sesion.getFondoCaja() : BigDecimal.ZERO);
      params.put("OBSERVACIONES", sesion.getObservacionesCierre() != null ?
          sesion.getObservacionesCierre() : "Sin observaciones");
      params.put("MOVIMIENTOS_DS", new JRBeanCollectionDataSource(movimientos));
      params.put("DATAFONOS_DS", new JRBeanCollectionDataSource(datafonos));
      params.put("DENOMINACIONES_DS", new JRBeanCollectionDataSource(denominaciones));

      byte[] pdf = pdfGeneratorService.generarPdf("cierre_caja_ticket", params, new ArrayList<>());

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline", "cierre_caja_ticket_" + id + ".pdf");

      log.info("✅ Cierre de caja generado exitosamente para sesión {}", id);
      return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("❌ Error generando cierre de caja para sesión {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(("Error: " + e.getMessage()).getBytes());
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

  @Operation(summary = "Obtener reporte de cierre en HTML")
  @GetMapping("/{id}/reporte-cierre")
  @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
  public ResponseEntity<String> obtenerReporteCierre(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean incluirFacturas,
      @RequestParam(defaultValue = "false") boolean incluirDenominaciones,
      @RequestParam(defaultValue = "false") boolean incluirDatafonos,
      @RequestParam(defaultValue = "false") boolean incluirMovimientos,
      @RequestParam(defaultValue = "false") boolean incluirPlataformas,
      HttpServletRequest request) {

    try {
      String token = request.getHeader("Authorization").substring(7);
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (!securityContextService.isSupervisor() &&
          !sesion.getUsuario().getId().equals(usuarioId)) {
        return ResponseEntity.status(403)
            .body("<html><body><h1>No tiene permisos para ver este reporte</h1></body></html>");
      }

      OpcionesImpresionCierreDTO opciones = OpcionesImpresionCierreDTO.builder()
          .incluirFacturas(incluirFacturas)
          .incluirDenominaciones(incluirDenominaciones)
          .incluirDatafonos(incluirDatafonos)
          .incluirMovimientos(incluirMovimientos)
          .incluirPlataformas(incluirPlataformas)
          .build();

      String html = sesionCajaService.generarHtmlCierre(id, opciones);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
          .body(html);

    } catch (Exception e) {
      log.error("Error generando reporte de cierre: {}", e.getMessage());
      return ResponseEntity.status(500)
          .body("<html><body><h1>Error: " + e.getMessage() + "</h1></body></html>");
    }
  }

  @PostMapping("/{id}/cierre/enviar-email")
  @PreAuthorize("hasAnyRole('ROOT','SUPER_ADMIN','ADMIN','JEFE_CAJAS','CAJERO')")
  public ResponseEntity<ApiResponse<Void>> enviarCierrePorEmail(
      @PathVariable Long id,
      @RequestBody(required = false) Map<String, String> body,
      HttpServletRequest httpRequest) {

    log.info("📧 Solicitud de envío de email para cierre sesión: {}", id);

    try {
      String emailAdicional = body != null ? body.get("emailAdicional") : null;

      OpcionesImpresionCierreDTO opciones = new OpcionesImpresionCierreDTO();
      opciones.setIncluirDetalle(true);
      opciones.setIncluirMovimientos(true);
      opciones.setIncluirDenominaciones(true);

      sesionCajaService.enviarEmailCierre(id, opciones, emailAdicional);

      return ResponseEntity.ok(ApiResponse.<Void>builder()
          .success(true)
          .message("Email de cierre enviado exitosamente")
          .build());

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.<Void>builder().success(false).message(e.getMessage()).build());
    } catch (Exception e) {
      log.error("❌ Error enviando email de cierre para sesión {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.<Void>builder()
              .success(false)
              .message("No se pudo enviar el email: " + e.getMessage())
              .build());
    }
  }

  // =========================================================================
  // LISTADOS
  // =========================================================================

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

  @Operation(summary = "Obtener todas las sesiones (Admin)")
  @GetMapping("/todas")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<List<SesionCajaResponse>>> obtenerTodasLasSesiones() {
    try {
      List<SesionCaja> sesiones = sesionCajaService.buscarTodas();

      List<SesionCajaResponse> response = sesiones.stream()
          .map(this::construirResponse)
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.ok("Sesiones obtenidas exitosamente", response));
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

      return ResponseEntity.ok(ApiResponse.ok("Sesiones " + estado + " obtenidas", response));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Estado inválido: " + estado));
    } catch (Exception e) {
      log.error("Error obteniendo sesiones por estado: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error: " + e.getMessage()));
    }
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

  @Operation(summary = "Obtener resumen detallado de sesión")
  @GetMapping("/{id}/resumen-detallado")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<ResumenCajaDetalladoDTO>> obtenerResumenDetallado(
      @PathVariable Long id,
      HttpServletRequest request) {

    try {
      ResumenCajaDetalladoDTO resumen = sesionCajaService.obtenerResumenDetallado(id);
      return ResponseEntity.ok(ApiResponse.ok("Resumen obtenido exitosamente", resumen));
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
          id, request.getMonto(), request.getConcepto());

      MovimientoCajaDTO response = MovimientoCajaDTO.builder()
          .id(vale.getId())
          .tipoMovimiento(vale.getTipoMovimiento().name())
          .monto(vale.getMonto())
          .concepto(vale.getConcepto())
          .fechaHora(vale.getFechaHora())
          .autorizadoPor(vale.getAutorizadoPorId())
          .build();

      return ResponseEntity.ok(ApiResponse.ok("Vale registrado exitosamente", response));
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

  @Operation(summary = "Obtener fondo de caja de última sesión cerrada")
  @GetMapping("/terminal/{terminalId}/ultimo-fondo-caja")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<FondoCajaResponse>> obtenerUltimoFondoCaja(
      @PathVariable Long terminalId) {

    try {
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

      return ResponseEntity.ok(ApiResponse.ok("Fondo de caja obtenido exitosamente", response));

    } catch (Exception e) {
      log.error("Error obteniendo fondo de caja para terminal {}: {}", terminalId, e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al obtener fondo de caja: " + e.getMessage()));
    }
  }

  // =========================================================================
  // MÉTODOS PRIVADOS
  // =========================================================================

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
    return token.substring(7);
  }

  private SesionCajaUsuarioDTO mapTurnoDTO(SesionCajaUsuario turno) {
    return SesionCajaUsuarioDTO.builder()
        .id(turno.getId())
        .sesionCajaId(turno.getSesionCaja().getId())
        .usuarioId(turno.getUsuario().getId())
        .usuarioNombre(turno.getUsuario().getNombre() + " " + turno.getUsuario().getApellidos())
        .fechaHoraInicio(turno.getFechaHoraInicio())
        .fechaHoraFin(turno.getFechaHoraFin())
        .estado(turno.getEstado())
        .ventasEfectivo(turno.getVentasEfectivo())
        .ventasTarjeta(turno.getVentasTarjeta())
        .ventasTransferencia(turno.getVentasTransferencia())
        .ventasOtros(turno.getVentasOtros())
        .montoEsperado(turno.getMontoEsperado())
        .montoContado(turno.getMontoContado())
        .diferencia(turno.getDiferencia())
        .observacionesCierre(turno.getObservacionesCierre())
        .fechaHoraInicioConteo(turno.getFechaHoraInicioConteo())
        .terminalId(turno.getSesionCaja().getTerminal().getId())
        .terminalNombre(turno.getSesionCaja().getTerminal().getNombre())
        .build();
  }

  private String obtenerMetodoPagoEstandar(String tipo) {
    if (tipo == null) return "S";
    return switch (tipo.toUpperCase()) {
      case "EFECTIVO", "E"                              -> "E";
      case "TARJETA", "TARJETA_CREDITO",
           "TARJETA_DEBITO", "TC", "TD"                -> "TC";
      case "TRANSFERENCIA", "TB"                        -> "TB";
      case "SINPE_MOVIL", "SINPE", "S"                 -> "S";
      case "PLATAFORMA_DIGITAL"                         -> "S";
      default                                           -> "S";
    };
  }
}