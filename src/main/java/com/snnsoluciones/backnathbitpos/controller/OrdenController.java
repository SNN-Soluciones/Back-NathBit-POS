package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.orden.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Órdenes", description = "Gestión de órdenes/pedidos")
public class OrdenController {

    private final OrdenService ordenService;

    @Operation(summary = "Crear nueva orden")
    @PostMapping
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> crear(@Valid @RequestBody CrearOrdenRequest request) {
        try {
            OrdenResponse orden = ordenService.crearOrden(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Orden creada exitosamente", orden));
        } catch (Exception e) {
            log.error("Error al crear orden: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Obtener orden por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> obtener(@PathVariable Long id) {
        try {
            OrdenResponse orden = ordenService.obtenerOrden(id);
            return ResponseEntity.ok(ApiResponse.success("Orden encontrada", orden));
        } catch (Exception e) {
            log.error("Error al obtener orden: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Obtener orden activa por mesa")
    @GetMapping("/mesa/{mesaId}/activa")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> obtenerPorMesa(@PathVariable Long mesaId) {
        try {
            OrdenResponse orden = ordenService.obtenerOrdenActivaPorMesa(mesaId);
            return ResponseEntity.ok(ApiResponse.success("Orden activa encontrada", orden));
        } catch (Exception e) {
            log.error("Error al obtener orden por mesa: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Listar órdenes por sucursal")
    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<List<OrdenListResponse>>> listarPorSucursal(
            @PathVariable Long sucursalId,
            @RequestParam(required = false) EstadoOrden estado) {
        try {
            List<OrdenListResponse> ordenes = ordenService.listarOrdenesPorSucursal(sucursalId, estado);
            return ResponseEntity.ok(ApiResponse.success("Órdenes encontradas", ordenes));
        } catch (Exception e) {
            log.error("Error al listar órdenes: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Agregar item a orden")
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> agregarItem(
            @PathVariable Long id,
            @Valid @RequestBody AgregarItemRequest request) {
        try {
            OrdenResponse orden = ordenService.agregarItem(id, request);
            return ResponseEntity.ok(ApiResponse.success("Item agregado exitosamente", orden));
        } catch (Exception e) {
            log.error("Error al agregar item: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar item de orden")
    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> actualizarItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ActualizarItemRequest request) {
        try {
            OrdenResponse orden = ordenService.actualizarItem(id, itemId, request);
            return ResponseEntity.ok(ApiResponse.success("Item actualizado", orden));
        } catch (Exception e) {
            log.error("Error al actualizar item: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar item de orden")
    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> eliminarItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        try {
            OrdenResponse orden = ordenService.eliminarItem(id, itemId);
            return ResponseEntity.ok(ApiResponse.success("Item eliminado", orden));
        } catch (Exception e) {
            log.error("Error al eliminar item: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Enviar orden a cocina")
    @PostMapping("/{id}/enviar-cocina")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> enviarCocina(@PathVariable Long id) {
        try {
            OrdenResponse orden = ordenService.enviarCocina(id);
            return ResponseEntity.ok(ApiResponse.success("Orden enviada a cocina", orden));
        } catch (Exception e) {
            log.error("Error al enviar a cocina: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cambiar estado de orden")
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoOrdenRequest request) {
        try {
            OrdenResponse orden = ordenService.cambiarEstado(id, request);
            return ResponseEntity.ok(ApiResponse.success("Estado actualizado", orden));
        } catch (Exception e) {
            log.error("Error al cambiar estado: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Obtener órdenes para cocina")
    @GetMapping("/cocina/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<List<OrdenCocinaResponse>>> obtenerParaCocina(@PathVariable Long sucursalId) {
        try {
            List<OrdenCocinaResponse> ordenes = ordenService.obtenerOrdenesParaCocina(sucursalId);
            return ResponseEntity.ok(ApiResponse.success("Órdenes de cocina", ordenes));
        } catch (Exception e) {
            log.error("Error al obtener órdenes para cocina: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Marcar item como preparado")
    @PutMapping("/{id}/items/{itemId}/preparado")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<Void>> marcarPreparado(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        try {
            // TODO: Implementar en el servicio
            return ResponseEntity.ok(ApiResponse.success("Item marcado como preparado", null));
        } catch (Exception e) {
            log.error("Error al marcar como preparado: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Marcar item como entregado")
    @PutMapping("/{id}/items/{itemId}/entregado")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<Void>> marcarEntregado(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        try {
            // TODO: Implementar en el servicio
            return ResponseEntity.ok(ApiResponse.success("Item marcado como entregado", null));
        } catch (Exception e) {
            log.error("Error al marcar como entregado: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Marcar orden como pagada")
    @PutMapping("/{id}/marcar-pagada")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<Void>> marcarComoPagada(
        @PathVariable Long id,
        @RequestParam(required = false) Long facturaId) {
        try {
            ordenService.marcarComoPagada(id, facturaId);
            return ResponseEntity.ok(ApiResponse.success("Orden marcada como pagada", null));
        } catch (Exception e) {
            log.error("Error al marcar orden como pagada: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar número de personas en la orden")
    @PatchMapping("/{id}/numero-personas")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN', 'ROOT', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrdenResponse>> actualizarNumeroPersonas(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarNumeroPersonasRequest request) {
        try {
            OrdenResponse orden = ordenService.actualizarNumeroPersonas(id, request);
            return ResponseEntity.ok(ApiResponse.success("Número de personas actualizado", orden));
        } catch (Exception e) {
            log.error("Error al actualizar número de personas: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Crear orden de ventanilla (sin mesa)")
    @PostMapping("/ventanilla")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN','ROOT', 'SUPER_ADMIN', 'COCINERO')")
    public ResponseEntity<ApiResponse<OrdenResponse>> crearOrdenVentanilla(
        @Valid @RequestBody CrearOrdenRequest request) {
        try {
            // Mapear a CrearOrdenRequest con mesaId = null
            CrearOrdenRequest ordenRequest = new CrearOrdenRequest(
                null,
                request.sucursalId(),
                request.clienteId(),  // Sin cliente
                request.nombreCliente(),
                1,     // 1 persona por defecto
                BigDecimal.ZERO,  // Sin servicio para llevar
                request.observaciones(),
                request.ordenNumero(),
                request.items(),
                null
            );

            OrdenResponse orden = ordenService.crearOrden(ordenRequest);
            return ResponseEntity.ok(ApiResponse.success("Orden de ventanilla creada", orden));
        } catch (Exception e) {
            log.error("Error al crear orden de ventanilla: ", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}