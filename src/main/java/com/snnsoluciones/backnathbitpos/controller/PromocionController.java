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
 * Base URL: /api/business/promociones
 */
@RestController
@RequestMapping("/promociones")
@RequiredArgsConstructor
@Slf4j
public class PromocionController {

    private final PromocionService promocionService;
    private final PromocionMotorService promocionMotorService;

    // =========================================================================
    // GET
    // =========================================================================

    /**
     * GET /api/business/promociones
     * Lista todas las promociones activas con sus items y alcance.
     */
    @GetMapping
    public ResponseEntity<List<PromocionDTO>> listarTodas() {
        log.info("GET /promociones - Listando todas las promociones activas");
        return ResponseEntity.ok(promocionService.listarTodas());
    }

    /**
     * GET /api/business/promociones/{id}
     * Obtener una promoción por ID con items y alcance completo.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromocionDTO> obtenerPorId(@PathVariable Long id) {
        log.info("GET /promociones/{} - Obteniendo promoción", id);
        try {
            return ResponseEntity.ok(promocionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            log.warn("Promoción ID {} no encontrada", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/business/promociones/dia/{dia}
     * Promos activas para un día comercial específico.
     *
     * @param dia  Día en mayúsculas: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
     *
     * Ejemplo: GET /promociones/dia/VIERNES
     */
    @GetMapping("/dia/{dia}")
    public ResponseEntity<List<PromocionDTO>> listarPorDia(@PathVariable String dia) {
        log.info("GET /promociones/dia/{} - Listando promos activas", dia);
        try {
            return ResponseEntity.ok(promocionService.listarActivasPorDia(dia));
        } catch (IllegalArgumentException e) {
            log.warn("Error al listar por día: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/business/promociones/dia/{dia}/hora
     * Promos activas para un día y hora específicos.
     * Cubre el caso donde hora_inicio/hora_fin son NULL (aplica todo el día).
     *
     * @param dia   Día en mayúsculas: LUNES... DOMINGO
     * @param hora  Hora en formato HH:mm (ej: 22:00)
     *
     * Ejemplo: GET /promociones/dia/VIERNES/hora?hora=22:00
     */
    @GetMapping("/dia/{dia}/hora")
    public ResponseEntity<List<PromocionDTO>> listarPorDiaYHora(
            @PathVariable String dia,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora) {
        log.info("GET /promociones/dia/{}/hora?hora={} - Listando promos activas", dia, hora);
        try {
            return ResponseEntity.ok(promocionService.listarActivasPorDiaYHora(dia, hora));
        } catch (IllegalArgumentException e) {
            log.warn("Error al listar por día y hora: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // POST
    // =========================================================================

    /**
     * POST /api/business/promociones
     * Crear una nueva promoción con items y alcance.
     */
    @PostMapping
    public ResponseEntity<PromocionDTO> crear(@Valid @RequestBody CreatePromocionRequest request) {
        log.info("POST /promociones - Creando promoción: {}", request.getNombre());
        try {
            PromocionDTO creada = promocionService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IllegalArgumentException e) {
            log.error("Error al crear promoción: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // PUT
    // =========================================================================

    /**
     * PUT /api/business/promociones/{id}
     * Actualizar una promoción completa.
     * Reemplaza items y alcance (familias, categorías, productos) por completo.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PromocionDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromocionRequest request) {
        log.info("PUT /promociones/{} - Actualizando promoción", id);
        try {
            PromocionDTO actualizada = promocionService.actualizar(id, request);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            log.error("Error al actualizar promoción {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // =========================================================================
    // PATCH
    // =========================================================================

    /**
     * PATCH /api/business/promociones/{id}/estado
     * Activar o desactivar una promoción sin modificar nada más.
     *
     * Ejemplo: PATCH /promociones/5/estado?activo=false
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PromocionDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {
        log.info("PATCH /promociones/{}/estado?activo={} - Cambiando estado", id, activo);
        try {
            return ResponseEntity.ok(promocionService.cambiarEstado(id, activo));
        } catch (IllegalArgumentException e) {
            log.error("Error al cambiar estado de promoción {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/business/promociones/producto/{productoId}
     *
     * Devuelve las promociones activas que aplican a un producto,
     * considerando su categoría, familia, día comercial y hora.
     *
     * El frontend usa esta respuesta para validar el carrito y
     * aplicar la promo correspondiente.
     *
     * Query params:
     * @param categoriaId  ID de la categoría del producto (opcional)
     * @param familiaId    ID de la familia del producto (opcional)
     * @param dia          Día comercial: LUNES...DOMINGO (obligatorio)
     * @param hora         Hora actual HH:mm (opcional, si no viene no filtra por horario)
     *
     * Ejemplo:
     * GET /promociones/producto/42?categoriaId=7&familiaId=3&dia=VIERNES&hora=22:00
     *
     * Respuesta vacía [] = no hay promos activas para este producto ahora.
     */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<PromocionDTO>> buscarParaProducto(
        @PathVariable Long productoId,
        @RequestParam(required = false) Long categoriaId,
        @RequestParam(required = false) Long familiaId,
        @RequestParam String dia,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "HH:mm") LocalTime hora) {

        log.info("GET /promociones/producto/{} - dia={} hora={} cat={} fam={}",
            productoId, dia, hora, categoriaId, familiaId);

        try {
            List<PromocionDTO> promos = promocionService.buscarParaProducto(
                productoId, categoriaId, familiaId, dia, hora);

            return ResponseEntity.ok(promos);

        } catch (IllegalArgumentException e) {
            log.warn("Error buscando promos para producto {}: {}", productoId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Agregar estos 3 endpoints al final de la clase ────────────────────

    /**
     * POST /api/business/promociones/evaluar/{ordenId}
     *
     * Evalúa qué promociones califican para la orden actual.
     * Solo lectura — no persiste nada ni modifica la orden.
     *
     * El frontend lo llama cada vez que el mesero agrega o quita un ítem.
     * La respuesta muestra exactamente qué descuento tendría cada ítem
     * para que el mesero decida si aplicar o no.
     *
     * Respuesta vacía [] = ninguna promo activa califica para esta orden.
     */
    @PostMapping("/evaluar/{ordenId}")
    public ResponseEntity<List<PromocionAplicableDTO>> evaluar(
        @PathVariable Long ordenId) {
        log.info("POST /promociones/evaluar/{} - Evaluando promos", ordenId);
        try {
            return ResponseEntity.ok(promocionMotorService.evaluar(ordenId));
        } catch (IllegalArgumentException e) {
            log.warn("Error evaluando promos para orden {}: {}", ordenId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/business/promociones/aplicar/{ordenId}
     *
     * Aplica las promociones seleccionadas por el mesero a la orden.
     * Persiste los descuentos en los OrdenItems y activa el estado
     * de rondas para AYCE/BARRA_LIBRE.
     *
     * Body: { "promocionIds": [1, 3] }
     *
     * Valida stacking — si una promo no permite combinarse con otras
     * y se envían múltiples IDs, retorna 409.
     */
    @PostMapping("/aplicar/{ordenId}")
    public ResponseEntity<Void> aplicar(
        @PathVariable Long ordenId,
        @Valid @RequestBody AplicarPromocionesRequest request) {
        log.info("POST /promociones/aplicar/{} - Aplicando promos: {}",
            ordenId, request.getPromocionIds());
        try {
            promocionMotorService.aplicar(ordenId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error aplicando promos a orden {}: {}", ordenId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Conflicto aplicando promos a orden {}: {}", ordenId, e.getMessage());
            return ResponseEntity.status(409).build();
        }
    }

    /**
     * POST /api/business/promociones/nueva-ronda/{ordenId}
     *
     * Solicita una nueva ronda para un producto AYCE o BARRA_LIBRE.
     * Solo el mesero puede disparar esto explícitamente.
     *
     * Valida que:
     *   - La promo AYCE esté activa en la orden
     *   - El producto no haya llegado a su límite de rondas
     *
     * Si todo está bien, crea un nuevo OrdenItem a precio $0
     * e incrementa el contador de rondas.
     *
     * Body: { "promocionId": 1, "productoId": 5 }
     *
     * Retorna 409 si ya se alcanzó el máximo de rondas.
     */
    @PostMapping("/nueva-ronda/{ordenId}")
    public ResponseEntity<Void> nuevaRonda(
        @PathVariable Long ordenId,
        @Valid @RequestBody NuevaRondaRequest request) {
        log.info("POST /promociones/nueva-ronda/{} - Producto: {}",
            ordenId, request.getProductoId());
        try {
            promocionMotorService.nuevaRonda(ordenId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error en nueva ronda orden {}: {}", ordenId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Límite de rondas alcanzado en orden {}: {}", ordenId, e.getMessage());
            return ResponseEntity.status(409).build();
        }
    }
}