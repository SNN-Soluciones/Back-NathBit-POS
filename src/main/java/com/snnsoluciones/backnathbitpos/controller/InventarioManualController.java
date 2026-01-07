package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.inventario.*;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioManualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión manual de inventarios
 * 
 * PERMISOS:
 * - Carga inicial: SUPER_ADMIN, ADMIN
 * - Ajustes: SUPER_ADMIN, ADMIN
 * - Consultas: SUPER_ADMIN, ADMIN, CAJERO (solo lectura)
 */
@RestController
@RequestMapping("/api/inventarios/manual")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventario Manual", description = "Gestión manual de inventarios (ajustes, carga inicial, kardex)")
@CrossOrigin(origins = "*")
public class InventarioManualController {

    private final ProductoInventarioManualService inventarioManualService;

    // ==================== CARGA INICIAL ====================

    @Operation(summary = "Carga inicial de inventarios en lote",
        description = "Permite cargar inventario inicial de múltiples productos en una sucursal")
    @PostMapping("/carga-inicial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventarioActualDTO>>> cargarInventarioInicial(
            @Valid @RequestBody CargaInicialInventarioDTO request) {

        log.info("Solicitud de carga inicial para sucursal: {}", request.getSucursalId());

        try {
            List<InventarioActualDTO> resultado = inventarioManualService.cargarInventarioInicial(request);

            String mensaje = String.format(
                "Inventario inicial cargado: %d productos procesados de %d solicitados",
                resultado.size(), request.getProductos().size()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mensaje, resultado));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en carga inicial: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error en carga inicial de inventario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al cargar inventario inicial: " + e.getMessage()));
        }
    }

    // ==================== AJUSTES INDIVIDUALES ====================

    @Operation(summary = "Ajustar inventario (entrada o salida manual)",
        description = "Permite ajustes manuales de inventario: entradas, salidas, mermas, etc.")
    @PostMapping("/ajuste")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventarioActualDTO>> ajustarInventario(
            @Valid @RequestBody AjusteInventarioDTO ajuste) {

        log.info("Solicitud de ajuste: Producto {} | Tipo {} | Cantidad {}",
            ajuste.getProductoId(), ajuste.getTipoMovimiento(), ajuste.getCantidad());

        try {
            InventarioActualDTO resultado = inventarioManualService.ajustarInventario(ajuste);

            return ResponseEntity.ok(
                ApiResponse.ok("Ajuste de inventario realizado exitosamente", resultado)
            );

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en ajuste: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error en ajuste de inventario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al ajustar inventario: " + e.getMessage()));
        }
    }

    // ==================== CONSULTAS KARDEX ====================

    @Operation(summary = "Obtener kardex de un producto en una sucursal",
        description = "Historial completo de movimientos de un producto")
    @GetMapping("/kardex/producto/{productoId}/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<Page<MovimientoInventarioDTO>>> obtenerKardex(
            @PathVariable Long productoId,
            @PathVariable Long sucursalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Consultando kardex: Producto {} | Sucursal {}", productoId, sucursalId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MovimientoInventarioDTO> kardex = inventarioManualService.obtenerKardex(
                productoId, sucursalId, pageable
            );

            return ResponseEntity.ok(ApiResponse.ok("Kardex obtenido", kardex));

        } catch (Exception e) {
            log.error("Error obteniendo kardex", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener kardex: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener todos los movimientos de una sucursal",
        description = "Historial de movimientos de inventario de todos los productos")
    @GetMapping("/movimientos/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<MovimientoInventarioDTO>>> obtenerMovimientosSucursal(
            @PathVariable Long sucursalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("Consultando movimientos de sucursal {}", sucursalId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MovimientoInventarioDTO> movimientos = 
                inventarioManualService.obtenerMovimientosPorSucursal(sucursalId, pageable);

            return ResponseEntity.ok(ApiResponse.ok("Movimientos obtenidos", movimientos));

        } catch (Exception e) {
            log.error("Error obteniendo movimientos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener movimientos: " + e.getMessage()));
        }
    }

    // ==================== CONSULTAS INVENTARIO ACTUAL ====================

    @Operation(summary = "Obtener inventario actual de una sucursal",
        description = "Listado completo de productos con su stock actual")
    @GetMapping("/actual/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<InventarioActualDTO>>> obtenerInventarioSucursal(
            @PathVariable Long sucursalId) {

        log.debug("Consultando inventario actual de sucursal {}", sucursalId);

        try {
            List<InventarioActualDTO> inventarios = 
                inventarioManualService.obtenerInventariosPorSucursal(sucursalId);

            return ResponseEntity.ok(
                ApiResponse.ok("Inventario obtenido", inventarios)
            );

        } catch (Exception e) {
            log.error("Error obteniendo inventario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener inventario: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener productos con stock bajo",
        description = "Productos que están por debajo del stock mínimo configurado")
    @GetMapping("/bajo-minimo/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<InventarioActualDTO>>> obtenerProductosBajoMinimo(
            @PathVariable Long sucursalId) {

        log.debug("Consultando productos bajo mínimo en sucursal {}", sucursalId);

        try {
            List<InventarioActualDTO> productos = 
                inventarioManualService.obtenerProductosBajoMinimo(sucursalId);

            return ResponseEntity.ok(
                ApiResponse.ok("Productos bajo mínimo obtenidos", productos)
            );

        } catch (Exception e) {
            log.error("Error obteniendo productos bajo mínimo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener productos: " + e.getMessage()));
        }
    }

    // ==================== UTILIDADES ====================

    @Operation(summary = "Tipos de movimiento disponibles",
        description = "Lista de tipos de movimiento válidos para ajustes manuales")
    @GetMapping("/tipos-movimiento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TipoMovimientoInfo>>> obtenerTiposMovimiento() {
        List<TipoMovimientoInfo> tipos = List.of(
            // ENTRADAS
            new TipoMovimientoInfo("ENTRADA_INICIAL", "Inventario inicial", true, "Para carga inicial de stock"),
            new TipoMovimientoInfo("ENTRADA_COMPRA", "Entrada por compra", true, "Compra a proveedor"),
            new TipoMovimientoInfo("ENTRADA_AJUSTE", "Ajuste positivo", true, "Corrección de sobrantes"),
            new TipoMovimientoInfo("ENTRADA_DEVOLUCION", "Devolución de cliente", true, "Cliente devuelve producto"),
            new TipoMovimientoInfo("ENTRADA_PRODUCCION", "Producción", true, "Producto terminado"),

            // SALIDAS
            new TipoMovimientoInfo("SALIDA_AJUSTE", "Ajuste negativo", false, "Corrección de faltantes"),
            new TipoMovimientoInfo("SALIDA_MERMA", "Merma", false, "Pérdida por vencimiento, daño, etc."),
            new TipoMovimientoInfo("SALIDA_CONSUMO", "Consumo interno", false, "Uso interno del negocio"),
            new TipoMovimientoInfo("SALIDA_DEVOLUCION", "Devolución a proveedor", false, "Devolver a proveedor")
        );

        return ResponseEntity.ok(ApiResponse.ok("Tipos de movimiento", tipos));
    }

    // Clase helper para respuesta de tipos
    record TipoMovimientoInfo(String codigo, String nombre, Boolean esEntrada, String descripcion) {}
}