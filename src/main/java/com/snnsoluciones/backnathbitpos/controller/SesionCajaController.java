package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.*;
import com.snnsoluciones.backnathbitpos.dto.sesiones.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.sesiones.RegistrarValeRequest;
import com.snnsoluciones.backnathbitpos.dto.sesiones.ResumenCajaDetalladoDTO;
import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.MovimientoCajaService;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<CierreCajaResponse>> cerrarSesion(
      @PathVariable Long id,
      @Valid @RequestBody CerrarSesionRequest request,
      HttpServletRequest httpRequest) {

    try {
      String token = httpRequest.getHeader("Authorization").substring(7);
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);

      // Verificar que la sesión existe
      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      // Validar permisos (cajero solo su propia sesión, supervisores cualquiera)
      if (!securityContextService.isSupervisor() &&
          !sesion.getUsuario().getId().equals(usuarioId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("No tiene permisos para cerrar esta sesión"));
      }

      // Obtener monto esperado antes de cerrar
      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      BigDecimal diferencia = request.getMontoCierre().subtract(montoEsperado);

      // Si hay diferencia significativa y no es supervisor
      BigDecimal umbral = new BigDecimal("10000"); // ₡10,000 de tolerancia
      if (diferencia.abs().compareTo(umbral) > 0 && !securityContextService.isSupervisor()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(String.format(
                "Diferencia de ₡%.2f requiere autorización. Esperado: ₡%.2f, Cierre: ₡%.2f",
                diferencia, montoEsperado, request.getMontoCierre()
            )));
      }

      // Cerrar sesión
      SesionCaja sesionCerrada = sesionCajaService.cerrarSesion(
          id,
          request.getMontoCierre(),
          request.getObservaciones()
      );

      // Construir respuesta mejorada
      CierreCajaResponse response = CierreCajaResponse.builder()
          .sesionId(sesionCerrada.getId())
          .fechaApertura(sesionCerrada.getFechaHoraApertura())
          .fechaCierre(sesionCerrada.getFechaHoraCierre())
          .montoInicial(sesionCerrada.getMontoInicial())
          .totalVentas(sesionCerrada.getTotalVentas())
          .totalDevoluciones(sesionCerrada.getTotalDevoluciones())
          .totalVales(movimientoCajaService.obtenerTotalVales(id)) // NUEVO
          .montoEsperado(montoEsperado) // CALCULADO
          .montoCierre(sesionCerrada.getMontoCierre())
          .diferencia(diferencia) // CALCULADA
          .cantidadFacturas(sesionCerrada.getCantidadFacturas())
          .cantidadTiquetes(sesionCerrada.getCantidadTiquetes())
          .cantidadNotasCredito(sesionCerrada.getCantidadNotasCredito())
          .totalEfectivo(sesionCerrada.getTotalEfectivo())
          .totalTarjeta(sesionCerrada.getTotalTarjeta())
          .totalTransferencia(sesionCerrada.getTotalTransferencia())
          .observaciones(sesionCerrada.getObservacionesCierre())
          .build();

      return ResponseEntity.ok(ApiResponse.ok(
          "Sesión cerrada exitosamente",
          response
      ));

    } catch (Exception e) {
      log.error("Error cerrando sesión: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al cerrar sesión: " + e.getMessage()));
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

  // Agregar en SesionCajaController.java

  @Operation(summary = "Obtener resumen detallado de sesión")
  @GetMapping("/{id}/resumen-detallado")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<ResumenCajaDetalladoDTO>> obtenerResumenDetallado(
      @PathVariable Long id,
      HttpServletRequest request) {

    try {
      // Validar acceso
      Long usuarioId = jwtTokenProvider.getUserIdFromToken(getToken(request));

      // Obtener resumen detallado
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
      // Solo supervisores pueden autorizar vales
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

  // Métodos helper

  private Long obtenerUsuarioIdDelToken(HttpServletRequest request) {
    // TODO: Implementar extracción real del JWT
    return 1L;
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
    token = token.substring(7, token.length());
    return token;
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

  @Operation(summary = "Cerrar sesión administrativamente")
  @PostMapping("/{id}/cerrar-admin")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
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
          .observaciones(observacionesAdmin)
          .build();

      // Obtener monto esperado antes de cerrar
      BigDecimal montoEsperado = sesionCajaService.calcularMontoEsperado(sesion);
      BigDecimal diferencia = request.getMontoCierre().subtract(montoEsperado);

      // Cerrar sesión (sin validación de umbral para admin)
      SesionCaja sesionCerrada = sesionCajaService.cerrarSesionAdmin(
          id,
          request.getMontoCierre(),
          observacionesAdmin
      );

      // Construir respuesta
      CierreCajaResponse response = CierreCajaResponse.builder()
          .sesionId(sesionCerrada.getId())
          .fechaApertura(sesionCerrada.getFechaHoraApertura())
          .fechaCierre(sesionCerrada.getFechaHoraCierre())
          .montoInicial(sesionCerrada.getMontoInicial())
          .totalVentas(sesionCerrada.getTotalVentas())
          .totalDevoluciones(sesionCerrada.getTotalDevoluciones())
          .totalVales(movimientoCajaService.obtenerTotalVales(id))
          .montoEsperado(montoEsperado)
          .montoCierre(sesionCerrada.getMontoCierre())
          .diferencia(diferencia)
          .cantidadFacturas(sesionCerrada.getCantidadFacturas())
          .cantidadTiquetes(sesionCerrada.getCantidadTiquetes())
          .cantidadNotasCredito(sesionCerrada.getCantidadNotasCredito())
          .totalEfectivo(sesionCerrada.getTotalEfectivo())
          .totalTarjeta(sesionCerrada.getTotalTarjeta())
          .totalTransferencia(sesionCerrada.getTotalTransferencia())
          .observaciones(sesionCerrada.getObservacionesCierre())
          .build();

      // Log de auditoría
      log.info("Cierre administrativo de sesión {} por usuario {} - Diferencia: {}",
          id, usuarioAdminId, diferencia);

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
}