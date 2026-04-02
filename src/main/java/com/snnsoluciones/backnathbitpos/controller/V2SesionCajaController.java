// src/main/java/com/snnsoluciones/backnathbitpos/controller/V2SesionCajaController.java

package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.v2sesion.*;
import com.snnsoluciones.backnathbitpos.entity.V2TurnoCajero;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.V2ReporteCajaService;
import com.snnsoluciones.backnathbitpos.service.V2SesionCajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/sesiones-caja")
@RequiredArgsConstructor
@Tag(name = "Sesiones de Caja v2", description = "Gestión de sesiones y turnos de caja")
public class V2SesionCajaController {

    private final V2SesionCajaService service;
    private final V2ReporteCajaService reporteService;
    private final JwtTokenProvider jwt;

    private Long usuarioId(HttpServletRequest req) {
        return jwt.getUserIdFromToken(req.getHeader("Authorization").substring(7));
    }

    // ── Sesión ────────────────────────────────────────────────

    @Operation(summary = "Abrir sesión de caja")
    @PostMapping("/abrir")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT')")
    public ResponseEntity<ApiResponse<V2AbrirSesionResponse>> abrirSesion(
        @Valid @RequestBody V2AbrirSesionRequest request,
        HttpServletRequest req) {
        try {
            V2AbrirSesionResponse response = service.abrirSesion(request, usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("Sesión abierta exitosamente", response));
        } catch (Exception e) {
            log.error("Error abriendo sesión v2: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Estado completo de una sesión")
    @GetMapping("/{sesionId}/estado")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<ApiResponse<V2EstadoSesionResponse>> obtenerEstado(
        @PathVariable Long sesionId,
        HttpServletRequest req) {
        try {
            Long usuarioId = usuarioId(req);
            V2EstadoSesionResponse estado = service.obtenerEstado(sesionId, usuarioId);
            return ResponseEntity.ok(ApiResponse.ok("Estado obtenido", estado));
        } catch (Exception e) {
            log.error("Error obteniendo estado sesión v2 {}: {}", sesionId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Turnos ────────────────────────────────────────────────

    @Operation(summary = "Unirse a un turno en sesión existente")
    @PostMapping("/{sesionId}/turnos/unirse")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT')")
    public ResponseEntity<ApiResponse<V2TurnoResponse>> unirseATurno(
        @PathVariable Long sesionId,
        HttpServletRequest req) {
        try {
            V2TurnoResponse turno = service.unirseATurno(sesionId, usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("Turno iniciado exitosamente", turno));
        } catch (Exception e) {
            log.error("Error uniéndose a turno v2: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Mi turno activo")
    @GetMapping("/mi-turno-activo")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<ApiResponse<V2TurnoResponse>> miTurnoActivo(HttpServletRequest req) {
        try {
            V2TurnoResponse turno = service.obtenerMiTurnoActivo(usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("Turno activo encontrado", turno));
        } catch (Exception e) {
            log.error("Error obteniendo turno activo v2: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cerrar turno")
    @PostMapping("/turnos/{turnoId}/cerrar")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT')")
    public ResponseEntity<ApiResponse<V2CerrarTurnoResponse>> cerrarTurno(
        @PathVariable Long turnoId,
        @Valid @RequestBody V2CerrarTurnoRequest request,
        HttpServletRequest req) {
        try {
            V2CerrarTurnoResponse response = service.cerrarTurno(turnoId, request, usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("Turno cerrado exitosamente", response));
        } catch (Exception e) {
            log.error("Error cerrando turno v2 {}: {}", turnoId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Movimientos ───────────────────────────────────────────

    @Operation(summary = "Registrar movimiento de caja")
    @PostMapping("/turnos/{turnoId}/movimientos")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT')")
    public ResponseEntity<ApiResponse<V2MovimientoResponse>> registrarMovimiento(
        @PathVariable Long turnoId,
        @Valid @RequestBody V2MovimientoRequest request,
        HttpServletRequest req) {
        try {
            V2MovimientoResponse response = service.registrarMovimiento(turnoId, request, usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("Movimiento registrado", response));
        } catch (Exception e) {
            log.error("Error registrando movimiento v2: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reporte HTML de sesión completa")
    @GetMapping("/{sesionId}/reporte")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<String> reporteSesion(
        @PathVariable Long sesionId,
        @RequestParam(defaultValue = "false") boolean incluirMovimientos,
        @RequestParam(defaultValue = "false") boolean incluirFacturas,
        @RequestParam(defaultValue = "false") boolean incluirDenominaciones,
        @RequestParam(defaultValue = "false") boolean incluirDatafonos,
        @RequestParam(defaultValue = "false") boolean incluirPlataformas,
        @RequestParam(defaultValue = "false") boolean incluirVentasCredito) {
        try {
            V2OpcionesReporteDTO opciones = V2OpcionesReporteDTO.builder()
                .incluirMovimientos(incluirMovimientos)
                .incluirFacturas(incluirFacturas)
                .incluirDenominaciones(incluirDenominaciones)
                .incluirDatafonos(incluirDatafonos)
                .incluirPlataformas(incluirPlataformas)
                .incluirVentasCredito(incluirVentasCredito)
                .build();
            String html = reporteService.generarHtmlSesion(sesionId, opciones);
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("<h1>Error: " + e.getMessage() + "</h1>");
        }
    }

    @Operation(summary = "Reporte HTML de turno individual")
    @GetMapping("/turnos/{turnoId}/reporte")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<String> reporteTurno(
        @PathVariable Long turnoId,
        @RequestParam(defaultValue = "false") boolean incluirMovimientos,
        @RequestParam(defaultValue = "false") boolean incluirDenominaciones,
        @RequestParam(defaultValue = "false") boolean incluirDatafonos,
        @RequestParam(defaultValue = "false") boolean incluirFacturas,
        @RequestParam(defaultValue = "false") boolean incluirVentasCredito) {
        try {
            V2OpcionesReporteDTO opciones = V2OpcionesReporteDTO.builder()
                .incluirMovimientos(incluirMovimientos)
                .incluirDenominaciones(incluirDenominaciones)
                .incluirDatafonos(incluirDatafonos)
                .incluirFacturas(incluirFacturas)
                .incluirVentasCredito(incluirVentasCredito)
                .build();
            String html = reporteService.generarHtmlTurno(turnoId, opciones);
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("<h1>Error: " + e.getMessage() + "</h1>");
        }
    }

    @Operation(summary = "Estado de sesión activa por terminal")
    @GetMapping("/terminal/{terminalId}/estado")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<ApiResponse<V2EstadoSesionResponse>> estadoPorTerminal(
        @PathVariable Long terminalId,
        HttpServletRequest req) {
        try {
            V2EstadoSesionResponse estado = service.obtenerEstadoPorTerminal(terminalId, usuarioId(req));
            return ResponseEntity.ok(ApiResponse.ok("OK", estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // En V2SesionCajaController.java
    @GetMapping("/terminal/{terminalId}/ultimo-fondo")
    @PreAuthorize("hasAnyRole('CAJERO','JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ultimoFondoV2(
        @PathVariable Long terminalId) {
        try {
            Map<String, Object> result = service.obtenerUltimoFondoTerminal(terminalId);
            return ResponseEntity.ok(ApiResponse.ok("OK", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Bitácora de sesiones v2 con filtros paginados")
    @GetMapping("/bitacora")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS','ADMIN','SUPER_ADMIN','ROOT','SOPORTE')")
    public ResponseEntity<ApiResponse<Page<V2BitacoraCajaSesionDTO>>> bitacora(
        @RequestParam(required = false) Long      sucursalId,
        @RequestParam(required = false) Long      terminalId,
        @RequestParam(required = false) Long      usuarioId,
        @RequestParam(required = false) String    estado,
        @RequestParam(required = false) String    modoGaveta,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
        @RequestParam(defaultValue = "0")    int    page,
        @RequestParam(defaultValue = "20")   int    size,
        @RequestParam(defaultValue = "fechaApertura") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            V2BitacoraCajaFilterRequest filtros = V2BitacoraCajaFilterRequest.builder()
                .sucursalId(sucursalId)
                .terminalId(terminalId)
                .usuarioId(usuarioId)
                .estado(estado)
                .modoGaveta(modoGaveta)
                .fechaDesde(fechaDesde)
                .fechaHasta(fechaHasta)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

            Page<V2BitacoraCajaSesionDTO> resultado = service.listarBitacora(filtros);
            return ResponseEntity.ok(ApiResponse.ok("Bitácora obtenida", resultado));
        } catch (Exception e) {
            log.error("Error en bitácora v2: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Helper privado ────────────────────────────────────────

    private V2EstadoSesionResponse.MiTurnoDTO buildMiTurno(
        Long turnoId, V2EstadoSesionResponse estado) {
        // Los valores ya están en otrosTurnos sin montos,
        // pero el service los tiene calculados — simplificar en v2.1
        return V2EstadoSesionResponse.MiTurnoDTO.builder()
            .turnoId(turnoId)
            .build();
    }
}