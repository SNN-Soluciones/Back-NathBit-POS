package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.AjusteInventarioDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.InventarioInicialDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.VerificarDisponibilidadDTO;
import com.snnsoluciones.backnathbitpos.entity.ProductoInventario;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductoInventarioController {

    private final ProductoInventarioService inventarioService;

    // Obtener inventario de una sucursal
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<ApiResponse<List<ProductoInventario>>> obtenerInventarioPorSucursal(@PathVariable Long sucursalId) {
        try {
            List<ProductoInventario> inventarios = inventarioService.obtenerInventarioPorSucursal(sucursalId);
            return ResponseEntity.ok(ApiResponse.ok("Inventarios obtenidos", inventarios));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener inventarios: " + e.getMessage()));
        }
    }

    // Obtener inventario de un producto en todas las sucursales
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<ApiResponse<List<ProductoInventario>>> obtenerInventarioPorProducto(@PathVariable Long productoId) {
        try {
            List<ProductoInventario> inventarios = inventarioService.obtenerInventarioPorProducto(productoId);
            return ResponseEntity.ok(ApiResponse.ok("Inventarios obtenidos", inventarios));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener inventarios: " + e.getMessage()));
        }
    }

    // Obtener inventario específico
    @GetMapping("/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<ApiResponse<ProductoInventario>> obtenerInventario(
        @PathVariable Long productoId,
        @PathVariable Long sucursalId) {
        try {
            ProductoInventario inventario = inventarioService.obtenerInventario(productoId, sucursalId);
            return ResponseEntity.ok(ApiResponse.ok("Inventario obtenido", inventario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Inventario no encontrado: " + e.getMessage()));
        }
    }

    // Crear inventario inicial
    @PostMapping("/inicializar")
    public ResponseEntity<ApiResponse<ProductoInventario>> inicializarInventario(@Valid @RequestBody InventarioInicialDTO dto) {
        try {
            ProductoInventario inventario = inventarioService.crearInventario(
                dto.getProductoId(),
                dto.getSucursalId(),
                dto.getCantidadMinima());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventario inicializado", inventario));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al crear inventario: " + e.getMessage()));
        }
    }

    // Ajustar inventario
    @PutMapping("/ajustar")
    public ResponseEntity<ApiResponse<ProductoInventario>> ajustarInventario(@Valid @RequestBody AjusteInventarioDTO dto) {
        try {
            ProductoInventario inventario = inventarioService.ajustarInventario(dto);
            return ResponseEntity.ok(ApiResponse.ok("Inventario ajustado", inventario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al ajustar inventario: " + e.getMessage()));
        }
    }

    // Obtener productos bajo mínimo
    @GetMapping("/alertas/sucursal/{sucursalId}")
    public ResponseEntity<ApiResponse<List<ProductoInventario>>> obtenerBajoMinimos(@PathVariable Long sucursalId) {
        try {
            List<ProductoInventario> alertas = inventarioService.obtenerBajoMinimos(sucursalId);
            String mensaje = alertas.isEmpty() ? "No hay productos bajo mínimo" : "Productos bajo mínimo encontrados";
            return ResponseEntity.ok(ApiResponse.ok(mensaje, alertas));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener alertas: " + e.getMessage()));
        }
    }

    // Verificar disponibilidad
    @PostMapping("/verificar-disponibilidad")
    public ResponseEntity<ApiResponse<Boolean>> verificarDisponibilidad(@Valid @RequestBody VerificarDisponibilidadDTO dto) {
        try {
            boolean disponible = inventarioService.verificarDisponibilidad(
                dto.getProductoId(),
                dto.getSucursalId(),
                dto.getCantidadRequerida());
            String mensaje = disponible ? "Stock disponible" : "Stock insuficiente";
            return ResponseEntity.ok(ApiResponse.ok(mensaje, disponible));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al verificar disponibilidad: " + e.getMessage()));
        }
    }
}