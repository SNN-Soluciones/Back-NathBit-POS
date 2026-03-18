package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.promociones.AplicarPromocionesRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.NuevaRondaRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.PromocionAplicableDTO;
import com.snnsoluciones.backnathbitpos.dto.promociones.PromocionDTO;
import com.snnsoluciones.backnathbitpos.service.promociones.PromocionMotorService;
import com.snnsoluciones.backnathbitpos.service.promociones.PromocionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Controller para Promociones.
 *
 * Base URL: /api/business/empresas/{empresaId}/sucursales/{sucursalId}/promociones
 *
 * sucursalId = 0 → promos globales de la empresa (sin sucursal específica).
 * Los endpoints del motor (evaluar/aplicar/nueva-ronda) reciben ordenId
 * porque la orden ya pertenece a empresa+sucursal.
 */
@RestController
@RequestMapping("/empresas/{empresaId}/sucursales/{sucursalId}/promociones")
@RequiredArgsConstructor
@Slf4j
public class PromocionController {

    private final PromocionService      promocionService;
    private final PromocionMotorService promocionMotorService;

    // =========================================================================
    // GET
    // =========================================================================

    @GetMapping
    public ResponseEntity<List<PromocionDTO>> listarTodas(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId) {
        log.info("GET /empresas/{}/sucursales/{}/promociones", empresaId, sucursalId);
        return ResponseEntity.ok(promocionService.listarTodas(empresaId, sucursalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionDTO> obtenerPorId(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long id) {
        log.info("GET /empresas/{}/sucursales/{}/promociones/{}", empresaId, sucursalId, id);
        try {
            return ResponseEntity.ok(promocionService.obtenerPorId(id, empresaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET .../promociones/dia/{dia}
     * Promos activas para un día comercial específico.
     * Día en mayúsculas: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
     */
    @GetMapping("/dia/{dia}")
    public ResponseEntity<List<PromocionDTO>> listarPorDia(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable String dia) {
        log.info("GET .../promociones/dia/{}", dia);
        try {
            return ResponseEntity.ok(
                promocionService.listarActivasPorDia(empresaId, sucursalId, dia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET .../promociones/dia/{dia}/hora?hora=22:00
     * Promos activas para un día y hora específicos.
     * Cubre el caso donde hora_inicio/hora_fin son NULL (aplica todo el día).
     */
    @GetMapping("/dia/{dia}/hora")
    public ResponseEntity<List<PromocionDTO>> listarPorDiaYHora(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable String dia,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora) {
        log.info("GET .../promociones/dia/{}/hora?hora={}", dia, hora);
        try {
            return ResponseEntity.ok(
                promocionService.listarActivasPorDiaYHora(empresaId, sucursalId, dia, hora));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET .../promociones/producto/{productoId}?categoriaId=7&familiaId=3&dia=VIERNES&hora=22:00
     *
     * Promos activas que aplican a un producto específico.
     * El frontend usa esto para mostrar al mesero qué promos aplican
     * cuando agrega un producto a la orden.
     */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<PromocionDTO>> buscarParaProducto(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long productoId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam String dia,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime hora) {
        log.info("GET .../promociones/producto/{} dia={} hora={}", productoId, dia, hora);
        try {
            return ResponseEntity.ok(
                promocionService.buscarParaProducto(
                    empresaId, sucursalId, productoId, categoriaId, familiaId, dia, hora));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // POST
    // =========================================================================

    @PostMapping
    public ResponseEntity<PromocionDTO> crear(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @Valid @RequestBody CreatePromocionRequest request) {
        log.info("POST .../promociones - Creando: {}", request.getNombre());
        try {
            // sucursalId = 0 se interpreta como promo global de empresa (sin sucursal)
            Long sucId = sucursalId == 0 ? null : sucursalId;
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(promocionService.crear(empresaId, sucId, request));
        } catch (IllegalArgumentException e) {
            log.error("Error creando promoción: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // PUT
    // =========================================================================

    @PutMapping("/{id}")
    public ResponseEntity<PromocionDTO> actualizar(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long id,
            @Valid @RequestBody CreatePromocionRequest request) {
        log.info("PUT .../promociones/{}", id);
        try {
            return ResponseEntity.ok(promocionService.actualizar(id, empresaId, request));
        } catch (IllegalArgumentException e) {
            log.error("Error actualizando promoción {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // PATCH
    // =========================================================================

    /**
     * PATCH .../promociones/{id}/estado?activo=false
     * Activar o desactivar una promoción.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PromocionDTO> cambiarEstado(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long id,
            @RequestParam boolean activo) {
        log.info("PATCH .../promociones/{}/estado?activo={}", id, activo);
        try {
            return ResponseEntity.ok(promocionService.cambiarEstado(id, empresaId, activo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // MOTOR — evaluar / aplicar / nueva-ronda
    // Estos 3 trabajan sobre una orden existente.
    // La URL incluye empresaId+sucursalId para que el motor filtre promos correctas.
    // =========================================================================

    /**
     * POST .../promociones/evaluar/{ordenId}
     *
     * Solo lectura — no persiste nada ni modifica la orden.
     * Devuelve qué promociones califican para la orden con:
     *   - itemsAfectados y totalDescuento para NXM/PORCENTAJE/etc.
     *   - productosBeneficioDisponibles para GRUPO_CONDICIONAL (lista al mesero)
     *   - itemsInicialesAYCE para AYCE/BARRA_LIBRE
     *
     * Llamar cada vez que el mesero agrega o quita un ítem.
     * Respuesta vacía [] = ninguna promo activa califica.
     */
    @PostMapping("/evaluar/{ordenId}")
    public ResponseEntity<List<PromocionAplicableDTO>> evaluar(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long ordenId) {
        log.info("POST .../promociones/evaluar/{}", ordenId);
        try {
            return ResponseEntity.ok(
                promocionMotorService.evaluar(ordenId, empresaId, sucursalId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST .../promociones/aplicar/{ordenId}
     *
     * Aplica las promociones seleccionadas por el mesero.
     * Persiste los descuentos en los OrdenItems.
     * Activa el estado de rondas para AYCE/BARRA_LIBRE.
     *
     * Body: { "promocionIds": [1, 3] }
     *
     * 409 → promo no permite stack con otras seleccionadas,
     *        o la promo ya no califica al momento de aplicar.
     */
    @PostMapping("/aplicar/{ordenId}")
    public ResponseEntity<Void> aplicar(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long ordenId,
            @Valid @RequestBody AplicarPromocionesRequest request) {
        log.info("POST .../promociones/aplicar/{} promos={}", ordenId, request.getPromocionIds());
        try {
            promocionMotorService.aplicar(ordenId, empresaId, sucursalId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).build();
        }
    }

    /**
     * POST .../promociones/nueva-ronda/{ordenId}
     *
     * Solicita una ronda adicional en un AYCE o BARRA_LIBRE activo.
     * Crea un OrdenItem a precio $0 e incrementa el contador de rondas.
     *
     * Body: { "promocionId": 1, "productoId": 5 }
     *
     * 409 → el producto ya alcanzó su máximo de rondas.
     */
    @PostMapping("/nueva-ronda/{ordenId}")
    public ResponseEntity<Void> nuevaRonda(
            @PathVariable Long empresaId,
            @PathVariable Long sucursalId,
            @PathVariable Long ordenId,
            @Valid @RequestBody NuevaRondaRequest request) {
        log.info("POST .../promociones/nueva-ronda/{} producto={}", ordenId, request.getProductoId());
        try {
            promocionMotorService.nuevaRonda(ordenId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).build();
        }
    }
}