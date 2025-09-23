package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoProveedorUpdateDto;
import com.snnsoluciones.backnathbitpos.service.ProductoProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos-proveedores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductoProveedorController {

  private final ProductoProveedorService productoProveedorService;

  // Obtener proveedores de un producto
  @GetMapping("/producto/{productoId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
  public ResponseEntity<ApiResponse<List<ProductoProveedorDto>>> obtenerProveedoresProducto(@PathVariable Long productoId) {
    try {
      List<ProductoProveedorDto> proveedores = productoProveedorService.obtenerProveedoresProducto(productoId);
      return ResponseEntity.ok(ApiResponse.ok("Proveedores obtenidos", proveedores));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al obtener proveedores: " + e.getMessage()));
    }
  }

  // Asociar proveedor a producto
  @PostMapping
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<ProductoProveedorDto>> asociarProveedor(@Valid @RequestBody ProductoProveedorCreateDto dto) {
    try {
      ProductoProveedorDto resultado = productoProveedorService.asociarProveedor(dto);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.ok("Proveedor asociado exitosamente", resultado));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Error al asociar proveedor: " + e.getMessage()));
    }
  }

  // Actualizar relación producto-proveedor
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<ProductoProveedorDto>> actualizar(
      @PathVariable Long id,
      @Valid @RequestBody ProductoProveedorUpdateDto dto) {
    try {
      ProductoProveedorDto actualizado = productoProveedorService.actualizar(id, dto);
      return ResponseEntity.ok(ApiResponse.ok("Actualizado exitosamente", actualizado));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Error al actualizar: " + e.getMessage()));
    }
  }

  // Activar/Desactivar relación
  @PutMapping("/{id}/toggle")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> toggleEstado(@PathVariable Long id) {
    try {
      productoProveedorService.toggleEstado(id);
      return ResponseEntity.ok(ApiResponse.ok("Estado actualizado exitosamente", null));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Error al cambiar estado: " + e.getMessage()));
    }
  }

  // Buscar por código de proveedor
  @GetMapping("/buscar")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
  public ResponseEntity<ApiResponse<ProductoProveedorDto>> buscarPorCodigo(
      @RequestParam Long proveedorId,
      @RequestParam String codigo) {
    try {
      ProductoProveedorDto resultado = productoProveedorService.buscarPorCodigoProveedor(proveedorId, codigo);
      return ResponseEntity.ok(ApiResponse.ok("Producto encontrado", resultado));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error("No se encontró el producto: " + e.getMessage()));
    }
  }

  // Eliminar relación (soft delete)
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
    try {
      productoProveedorService.eliminar(id);
      return ResponseEntity.ok(ApiResponse.ok("Eliminado exitosamente", null));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Error al eliminar: " + e.getMessage()));
    }
  }
}