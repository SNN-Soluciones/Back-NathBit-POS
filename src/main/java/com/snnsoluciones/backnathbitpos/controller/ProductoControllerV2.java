package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.ProductoServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v2/productos")
@RequiredArgsConstructor
@Tag(name = "Productos V2", description = "Gestión de productos - Versión 2 con soporte para combos y compuestos")
public class ProductoControllerV2 {

  private final ProductoServiceV2 productoServiceV2;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Crear un nuevo producto con imagen")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<ProductoDto>> crear(
      @RequestPart("producto") @Valid ProductoCreateDto dto,
      @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

    log.info("REST V2 - Creando producto tipo: {} - nombre: {} - empresaId: {} - sucursalId: {}",
        dto.getTipo(), dto.getNombre(), dto.getEmpresaId(), dto.getSucursalId());

    try {
      // Validar que venga empresaId en el DTO
      if (dto.getEmpresaId() == null) {
        throw new BusinessException("EmpresaId es requerido");
      }

      // Llamar al servicio con empresaId del DTO
      ProductoDto producto = productoServiceV2.crear(dto.getEmpresaId(), dto, imagen);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.<ProductoDto>builder()
              .success(true)
              .message("Producto creado exitosamente")
              .data(producto)
              .build());

    } catch (BusinessException e) {
      log.error("Error de negocio creando producto: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message(e.getMessage())
              .build());
    } catch (Exception e) {
      log.error("Error creando producto: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message("Error al crear producto: " + e.getMessage())
              .build());
    }
  }

  @GetMapping("/{id}/{empresaId}")
  @Operation(summary = "Obtener producto por ID")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO')")
  public ResponseEntity<ApiResponse<ProductoDto>> obtenerPorId(@PathVariable Long id,
      @PathVariable Long empresaId) {

    log.info("REST V2 - Buscando producto con ID: {}", id);

    try {
      ProductoDto producto = productoServiceV2.buscarPorId(empresaId, id);

      return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
          .success(true)
          .message("Producto encontrado")
          .data(producto)
          .build());

    } catch (Exception e) {
      log.error("Error obteniendo producto {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message("Producto no encontrado")
              .build());
    }
  }

  @Operation
  @GetMapping("/empresa-o-sucursal/{parametro}/id/{id}")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
  public ResponseEntity<ApiResponse<Page<ProductoDto>>> obtenerPorParametro(
      @PathVariable String parametro,
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDirection) {

    log.info("REST V2 - Buscando producto por parametro: {} y ID: {}", parametro, id);

    Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    if (parametro.equals("empresa")) {
      return ResponseEntity.ok().body(ApiResponse.ok(productoServiceV2.buscarPorEmpresa(id, pageable)));
    }
    if(parametro.equals("sucursal")) {
      return ResponseEntity.ok().body(ApiResponse.ok(productoServiceV2.buscarPorSucursal(id, pageable)));
    }

    return ResponseEntity.badRequest().body(ApiResponse.error("Parametro incorrecto"));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Actualizar producto existente con imagen opcional")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS')")
  public ResponseEntity<ApiResponse<ProductoDto>> actualizar(
      @PathVariable Long empresaId,
      @PathVariable Long id,
      @RequestPart("producto") @Valid ProductoUpdateDto dto,
      @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

    log.info("REST V2 - Actualizando producto ID: {} para empresa: {}", id, empresaId);

    if (imagen != null) {
      log.info("Imagen recibida: {} - tamaño: {} bytes",
          imagen.getOriginalFilename(), imagen.getSize());
    }

    try {
      ProductoDto producto = productoServiceV2.actualizar(empresaId, id, dto, imagen);

      return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
          .success(true)
          .message("Producto actualizado exitosamente")
          .data(producto)
          .build());

    } catch (ResourceNotFoundException e) {
      log.error("Producto no encontrado: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message(e.getMessage())
              .build());

    } catch (BusinessException e) {
      log.error("Error de negocio actualizando producto {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message(e.getMessage())
              .build());

    } catch (Exception e) {
      log.error("Error inesperado actualizando producto {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.<ProductoDto>builder()
              .success(false)
              .message("Error interno al actualizar producto")
              .build());
    }
  }

  @DeleteMapping("/{id}/{empresaId}")
  @Operation(summary = "Eliminar producto (cambiar estado a inactivo)")
  @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
  public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id,
      @PathVariable Long empresaId) {

    log.info("REST V2 - Eliminando producto ID: {}", id);

    try {
      productoServiceV2.eliminar(empresaId, id);

      return ResponseEntity.ok(ApiResponse.<Void>builder()
          .success(true)
          .message("Producto eliminado exitosamente")
          .build());

    } catch (Exception e) {
      log.error("Error eliminando producto {}: {}", id, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.<Void>builder()
              .success(false)
              .message("Error al eliminar: " + e.getMessage())
              .build());
    }
  }

  // Continuamos con más métodos...
}