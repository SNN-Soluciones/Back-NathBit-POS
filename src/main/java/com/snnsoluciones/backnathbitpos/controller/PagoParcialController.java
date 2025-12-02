package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.orden.MarcarItemsPagadosRequest;
import com.snnsoluciones.backnathbitpos.dto.orden.MarcarItemsPagadosResponse;
import com.snnsoluciones.backnathbitpos.dto.orden.OrdenEstadoPagosResponse;
import com.snnsoluciones.backnathbitpos.dto.orden.PagoParcialRequest;
import com.snnsoluciones.backnathbitpos.dto.orden.PagoParcialResponse;
import com.snnsoluciones.backnathbitpos.service.PagoParcialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para manejar pagos parciales de órdenes
 * 
 * Permite que múltiples personas paguen su parte de una orden
 * generando facturas individuales sin cerrar la orden hasta
 * que todos los items estén pagados.
 */
@Slf4j
@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
@Tag(name = "Pagos Parciales", description = "Gestión de pagos parciales en órdenes de restaurante")
public class PagoParcialController {

    private final PagoParcialService pagoParcialService;

    // =============================================
    // PROCESAR PAGO PARCIAL
    // =============================================

    @Operation(
        summary = "Procesar pago parcial de una orden",
        description = """
            Permite pagar items específicos de una orden sin cerrarla.
            
            **Flujo típico:**
            1. Mesa con orden de ₡25,000 (5 items)
            2. Persona 1 paga sus 2 items (₡8,000) → Genera factura
            3. Orden sigue abierta con 3 items pendientes
            4. Persona 2 paga sus items → Genera otra factura
            5. Cuando el último paga → Orden se cierra automáticamente
            
            **Tipos de documento soportados:**
            - `TI`: Tiquete Interno
            - `FI`: Factura Interna
            - `TE`: Tiquete Electrónico (próximamente)
            - `FE`: Factura Electrónica (próximamente)
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Pago parcial procesado exitosamente",
            content = @Content(schema = @Schema(implementation = PagoParcialResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o items ya pagados"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Orden o items no encontrados"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Orden ya cerrada o en estado inválido"
        )
    })
    @PostMapping("/{ordenId}/pago-parcial")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<PagoParcialResponse>> procesarPagoParcial(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable Long ordenId,
            @Valid @RequestBody PagoParcialRequest request) {
        
        log.info("📝 POST /api/v1/ordenes/{}/pago-parcial - Items: {}", 
            ordenId, request.getItemIds());

        try {
            PagoParcialResponse response = pagoParcialService.procesarPagoParcial(ordenId, request);
            
            log.info("✅ Pago parcial procesado - Orden: {}, Factura: {}, Items pagados: {}", 
                response.getOrdenNumero(),
                response.getNumeroInterno() != null ? response.getNumeroInterno() : response.getConsecutivo(),
                response.getItemsPagados().size());

            return ResponseEntity.ok(ApiResponse.success(response.getMensaje(), response));
            
        } catch (Exception e) {
            log.error("❌ Error procesando pago parcial para orden {}: {}", ordenId, e.getMessage());
            throw e; // Dejar que el GlobalExceptionHandler maneje
        }
    }

    // =============================================
    // CONSULTAR ESTADO DE PAGOS
    // =============================================

    @Operation(
        summary = "Obtener estado de pagos de una orden",
        description = """
            Retorna el estado detallado de pagos de una orden:
            - Items pendientes vs pagados
            - Total pagado vs pendiente
            - Facturas emitidas
            - Porcentaje de avance
            
            Útil para mostrar en la UI qué items faltan por pagar.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Estado de pagos obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = OrdenEstadoPagosResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        )
    })
    @GetMapping("/{ordenId}/estado-pagos")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<OrdenEstadoPagosResponse>> obtenerEstadoPagos(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable Long ordenId) {
        
        log.info("🔍 GET /api/v1/ordenes/{}/estado-pagos", ordenId);

        try {
            OrdenEstadoPagosResponse response = pagoParcialService.obtenerEstadoPagos(ordenId);
            
            log.info("✅ Estado de pagos obtenido - Orden: {}, Pagado: {}%, Items: {}/{}", 
                response.getOrdenNumero(),
                response.getPorcentajePagado(),
                response.getItemsPagados(),
                response.getItemsTotales());

            return ResponseEntity.ok(ApiResponse.success("Estado de pagos obtenido", response));
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo estado de pagos para orden {}: {}", ordenId, e.getMessage());
            throw e;
        }
    }

    // =============================================
    // OBTENER ITEMS PENDIENTES (HELPER)
    // =============================================

    @Operation(
        summary = "Obtener solo items pendientes de pago",
        description = "Retorna únicamente los items que aún no han sido pagados. Útil para la UI de selección de items a pagar."
    )
    @GetMapping("/{ordenId}/items-pendientes")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<OrdenEstadoPagosResponse>> obtenerItemsPendientes(
            @Parameter(description = "ID de la orden", required = true)
            @PathVariable Long ordenId) {
        
        log.info("🔍 GET /api/v1/ordenes/{}/items-pendientes", ordenId);

        try {
            OrdenEstadoPagosResponse response = pagoParcialService.obtenerEstadoPagos(ordenId);
            
            // Filtrar solo items pendientes
            var itemsPendientes = response.getItems().stream()
                .filter(item -> "PENDIENTE".equals(item.getEstadoPago()))
                .toList();
            
            response.setItems(itemsPendientes);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Items pendientes: " + itemsPendientes.size(), 
                response));
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo items pendientes para orden {}: {}", ordenId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/{ordenId}/marcar-items-pagados")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<MarcarItemsPagadosResponse>> marcarItemsPagados(
        @PathVariable Long ordenId,
        @RequestBody MarcarItemsPagadosRequest request) {

        log.info("📝 POST /api/v1/ordenes/{}/marcar-items-pagados - Items: {}",
            ordenId, request.getItemIds());

        MarcarItemsPagadosResponse response = pagoParcialService.marcarItemsPagados(ordenId, request);

        return ResponseEntity.ok(ApiResponse.success("Items marcados como pagados", response));
    }
}