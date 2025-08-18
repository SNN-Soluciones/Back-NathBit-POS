package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.sesion.*;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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

  @Operation(summary = "Cerrar sesión de caja")
  @PostMapping("/{id}/cerrar")
  @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
  public ResponseEntity<ApiResponse<CierreCajaResponse>> cerrarSesion(
      @PathVariable Long id,
      @Valid @RequestBody CerrarSesionRequest request) {

    try {
      // Verificar que la sesión pertenece al usuario
      SesionCaja sesion = sesionCajaService.buscarPorId(id)
          .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

      if (!sesion.puedeCerrarse()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("La sesión no puede cerrarse"));
      }

      // Cerrar sesión
      SesionCaja sesionCerrada = sesionCajaService.cerrarSesion(
          id,
          request.getMontoCierre(),
          request.getObservaciones()
      );

      // Construir respuesta con resumen
      CierreCajaResponse response = CierreCajaResponse.builder()
          .sesionId(sesionCerrada.getId())
          .fechaApertura(sesionCerrada.getFechaHoraApertura())
          .fechaCierre(sesionCerrada.getFechaHoraCierre())
          .montoInicial(sesionCerrada.getMontoInicial())
          .totalVentas(sesionCerrada.getTotalVentas())
          .totalDevoluciones(sesionCerrada.getTotalDevoluciones())
          .montoEsperado(sesionCerrada.calcularMontoEsperado())
          .montoCierre(sesionCerrada.getMontoCierre())
          .diferencia(sesionCerrada.getDiferenciaCierre())
          .cantidadFacturas(sesionCerrada.getCantidadFacturas())
          .cantidadTiquetes(sesionCerrada.getCantidadTiquetes())
          .cantidadNotasCredito(sesionCerrada.getCantidadNotasCredito())
          .totalEfectivo(sesionCerrada.getTotalEfectivo())
          .totalTarjeta(sesionCerrada.getTotalTarjeta())
          .totalTransferencia(sesionCerrada.getTotalTransferencia())
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
}