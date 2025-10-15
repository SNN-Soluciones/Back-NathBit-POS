// ==========================================
// 📍 ARCHIVO: controller/MovimientoCajaController.java
// ==========================================
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.movimiento.HistorialMovimientosResponse;
import com.snnsoluciones.backnathbitpos.dto.movimiento.MovimientoCajaDTO;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarEntradaRequest;
import com.snnsoluciones.backnathbitpos.dto.movimiento.RegistrarSalidaRequest;
import com.snnsoluciones.backnathbitpos.service.MovimientoCajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/movimientos-caja")
@RequiredArgsConstructor
@Tag(name = "Movimientos de Caja", description = "Gestión de movimientos de efectivo en caja")
public class MovimientoCajaController {

    private final MovimientoCajaService movimientoCajaService;

    /**
     * 🆕 Registrar salida de efectivo (Arqueo, Pago Proveedor, Otros)
     */
    @Operation(summary = "Registrar salida de efectivo")
    @PostMapping("/sesion/{sesionId}/salida")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<MovimientoCajaDTO>> registrarSalida(
            @PathVariable Long sesionId,
            @Valid @RequestBody RegistrarSalidaRequest request) {

        try {
            log.info("POST /api/movimientos-caja/sesion/{}/salida - Tipo: {}", 
                     sesionId, request.getTipoSalida());

            MovimientoCajaDTO response = movimientoCajaService.registrarSalida(sesionId, request);

            return ResponseEntity.ok(ApiResponse.ok(
                    "Salida de efectivo registrada exitosamente",
                    response
            ));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error registrando salida: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al registrar salida: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Registrar entrada de efectivo
     */
    @Operation(summary = "Registrar entrada de efectivo")
    @PostMapping("/sesion/{sesionId}/entrada")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<MovimientoCajaDTO>> registrarEntrada(
            @PathVariable Long sesionId,
            @Valid @RequestBody RegistrarEntradaRequest request) {

        try {
            log.info("POST /api/movimientos-caja/sesion/{}/entrada - Monto: {}", 
                     sesionId, request.getMonto());

            MovimientoCajaDTO response = movimientoCajaService.registrarEntrada(sesionId, request);

            return ResponseEntity.ok(ApiResponse.ok(
                    "Entrada de efectivo registrada exitosamente",
                    response
            ));

        } catch (Exception e) {
            log.error("Error registrando entrada: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al registrar entrada: " + e.getMessage()));
        }
    }

    /**
     * 🆕 Obtener historial completo de movimientos con totales
     */
    @Operation(summary = "Obtener historial completo de movimientos")
    @GetMapping("/sesion/{sesionId}/historial")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<HistorialMovimientosResponse>> obtenerHistorial(
            @PathVariable Long sesionId) {

        try {
            log.info("GET /api/movimientos-caja/sesion/{}/historial", sesionId);

            HistorialMovimientosResponse response = movimientoCajaService.obtenerHistorialCompleto(sesionId);

            return ResponseEntity.ok(ApiResponse.ok(response));

        } catch (Exception e) {
            log.error("Error obteniendo historial: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al obtener historial: " + e.getMessage()));
        }
    }

    /**
     * 📊 Obtener total de un tipo específico de movimiento
     */
    @Operation(summary = "Obtener total por tipo de movimiento")
    @GetMapping("/sesion/{sesionId}/total/{tipoMovimiento}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> obtenerTotalPorTipo(
            @PathVariable Long sesionId,
            @PathVariable String tipoMovimiento) {

        try {
            log.info("GET /api/movimientos-caja/sesion/{}/total/{}", sesionId, tipoMovimiento);

            java.math.BigDecimal total = movimientoCajaService.obtenerTotalSalidasPorTipo(
                    sesionId, 
                    tipoMovimiento
            );

            return ResponseEntity.ok(ApiResponse.ok(total));

        } catch (Exception e) {
            log.error("Error obteniendo total: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error al obtener total: " + e.getMessage()));
        }
    }

}