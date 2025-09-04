package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ActualizarPrecioDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoListDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.service.ProductoBusquedaService;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/productos")
@RequiredArgsConstructor
@Tag(name = "POS - Productos", description = "Búsqueda rápida de productos para punto de venta")
@PreAuthorize("hasAnyRole('CAJERO', 'MESERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
public class ProductoPOSController {

  private final ProductoBusquedaService productoBusquedaService;
  private final ProductoCrudService productoCrudService;

  @Operation(summary = "Buscar producto por código (interno o barras)")
  @GetMapping("/buscar-codigo")
  public ResponseEntity<ApiResponse<ProductoDto>> buscarPorCodigo(
      @RequestParam Long empresaId,
      @RequestParam String codigo) {

    try {
      // Primero intenta por código interno
      try {
        ProductoDto producto = productoBusquedaService.buscarPorCodigoInterno(empresaId, codigo);
        return ResponseEntity.ok(ApiResponse.ok("Producto encontrado", producto));
      } catch (Exception e) {
        // Si no encuentra, intenta por código de barras
        try {
          ProductoDto producto = productoBusquedaService.buscarPorCodigoBarras(empresaId, codigo);
          return ResponseEntity.ok(ApiResponse.ok("Producto encontrado", producto));
        } catch (Exception ex) {
          return ResponseEntity.ok(
              ApiResponse.error("Producto no encontrado con código: " + codigo));
        }
      }
    } catch (Exception e) {
      log.error("Error buscando producto por código: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al buscar producto: " + e.getMessage()));
    }
  }

  @Operation(summary = "Búsqueda rápida de productos (nombre, código, descripción)")
  @GetMapping("/busqueda-rapida")
  public ResponseEntity<ApiResponse<Page<ProductoListDto>>> busquedaRapida(
      @RequestParam Long empresaId,
      @RequestParam(required = false) String termino,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    try {
      Page<ProductoListDto> productos = productoBusquedaService.buscar(
          empresaId,
          termino,
          PageRequest.of(page, size)
      );

      String mensaje = productos.isEmpty()
          ? "No se encontraron productos"
          : "Se encontraron " + productos.getTotalElements() + " productos";

      return ResponseEntity.ok(ApiResponse.ok(mensaje, productos));

    } catch (Exception e) {
      log.error("Error en búsqueda rápida: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al buscar productos: " + e.getMessage()));
    }
  }

  @Operation(summary = "Listar productos por categoría")
  @GetMapping("/por-categoria/{categoriaId}")
  public ResponseEntity<ApiResponse<Page<ProductoListDto>>> listarPorCategoria(
      @PathVariable Long categoriaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    try {
      Page<ProductoListDto> productos = productoBusquedaService.listarPorCategoria(
          categoriaId,
          PageRequest.of(page, size)
      );

      return ResponseEntity.ok(ApiResponse.ok(productos));

    } catch (Exception e) {
      log.error("Error listando por categoría: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al listar productos: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener productos más vendidos (para sugerencias)")
  @GetMapping("/mas-vendidos")
  public ResponseEntity<ApiResponse<List<ProductoListDto>>> productosMasVendidos(
      @RequestParam Long empresaId,
      @RequestParam(defaultValue = "10") int limite) {

    // TODO: Implementar cuando tengamos el histórico de ventas
    return ResponseEntity.ok(ApiResponse.ok(
        "Funcionalidad pendiente de implementar",
        List.of()
    ));
  }

  @PatchMapping("/{empresaId}/{productoId}/precio")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Actualizar precio", description = "Actualiza únicamente el precio de un producto")
  public ResponseEntity<ApiResponse<ProductoDto>> actualizarPrecio(
      @PathVariable Long empresaId,
      @PathVariable Long productoId,
      @Valid @RequestBody ActualizarPrecioDto dto) {

    log.info("Actualizando precio del producto: {} de empresa: {} a {}",
        productoId, empresaId, dto.getPrecioVenta());

    ProductoDto productoActualizado = productoCrudService.actualizarPrecio(
        empresaId, productoId, dto);

    return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
        .success(true)
        .message("Precio actualizado exitosamente")
        .data(productoActualizado)
        .build());
  }
}