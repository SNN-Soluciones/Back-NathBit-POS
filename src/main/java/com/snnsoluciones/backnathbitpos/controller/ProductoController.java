package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import com.snnsoluciones.backnathbitpos.service.ProductoImagenService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos")
public class ProductoController {

  private final ProductoCrudService productoCrudService;
  private final ProductoImagenService productoImagenService;
  private final SucursalService sucursalService;

    @PostMapping(value = "/{empresaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'SOPORTE')")
    @Operation(summary = "Crear producto con imagen", description = "Crea un nuevo producto con imagen opcional")
    public ResponseEntity<ApiResponse<ProductoDto>> crearConImagen(
        @PathVariable Long empresaId,
        @RequestPart("producto") @Valid ProductoCreateDto dto,
        @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

        log.info("Creando producto para empresa: {} con imagen: {}",
            empresaId, imagen != null ? imagen.getOriginalFilename() : "sin imagen");

        ProductoDto producto = productoCrudService.crear(empresaId, dto, imagen);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<ProductoDto>builder()
                .success(true)
                .message("Producto creado exitosamente")
                .data(producto)
                .build());
    }

  @PutMapping(value = "/{empresaId}/{productoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Actualizar producto con imagen", description = "Actualiza un producto con imagen opcional")
  public ResponseEntity<ApiResponse<ProductoDto>> actualizarConImagen(
      @PathVariable Long empresaId,
      @PathVariable Long productoId,
      @RequestPart("producto") @Valid ProductoUpdateDto dto,
      @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

    log.info("Actualizando producto: {} de empresa: {} con imagen: {}",
        productoId, empresaId, imagen != null ? imagen.getOriginalFilename() : "sin cambios");

    ProductoDto producto = productoCrudService.actualizar(empresaId, productoId, dto, imagen);

    return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
        .success(true)
        .message("Producto actualizado exitosamente")
        .data(producto)
        .build());
  }

  @DeleteMapping("/{empresaId}/{productoId}/imagen")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Eliminar imagen", description = "Elimina la imagen de un producto")
  public ResponseEntity<ApiResponse<Void>> eliminarImagen(
      @PathVariable Long empresaId,
      @PathVariable Long productoId) {

    log.info("Eliminando imagen del producto: {} de empresa: {}", productoId, empresaId);
    productoImagenService.eliminarImagen(empresaId, productoId);

    return ResponseEntity.ok(ApiResponse.<Void>builder()
        .success(true)
        .message("Imagen eliminada exitosamente")
        .build());
  }

  @GetMapping("/{empresaId}/{productoId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
  @Operation(summary = "Obtener producto", description = "Obtiene un producto por ID")
  public ResponseEntity<ApiResponse<ProductoDto>> obtenerPorId(
      @PathVariable Long empresaId,
      @PathVariable Long productoId) {

    ProductoDto producto = productoCrudService.obtenerPorId(empresaId, productoId);

    return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
        .success(true)
        .message("Producto encontrado")
        .data(producto)
        .build());
  }

  @DeleteMapping("/{empresaId}/{productoId}")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Eliminar producto", description = "Elimina un producto")
  public ResponseEntity<ApiResponse<Void>> eliminar(
      @PathVariable Long empresaId,
      @PathVariable Long productoId) {

    log.info("Eliminando producto: {} de empresa: {}", productoId, empresaId);
    productoCrudService.eliminar(empresaId, productoId);

    return ResponseEntity.ok(ApiResponse.<Void>builder()
        .success(true)
        .message("Producto eliminado exitosamente")
        .build());
  }

  @PatchMapping("/{empresaId}/{productoId}/activar")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Activar/Desactivar producto", description = "Cambia el estado activo del producto")
  public ResponseEntity<ApiResponse<Void>> activarDesactivar(
      @PathVariable Long empresaId,
      @PathVariable Long productoId,
      @RequestParam boolean activo) {

    log.info("{} producto: {} de empresa: {}", activo ? "Activando" : "Desactivando", productoId,
        empresaId);
    productoCrudService.activarDesactivar(empresaId, productoId, activo);

    return ResponseEntity.ok(ApiResponse.<Void>builder()
        .success(true)
        .message("Producto " + (activo ? "activado" : "desactivado") + " exitosamente")
        .build());
  }

  @GetMapping("/{empresaId}/generar-codigo")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Generar código interno", description = "Genera un código interno único para producto")
  public ResponseEntity<ApiResponse<String>> generarCodigo(@PathVariable Long empresaId) {

    String codigo = productoCrudService.generarCodigoInterno(empresaId);

    return ResponseEntity.ok(ApiResponse.<String>builder()
        .success(true)
        .message("Código generado")
        .data(codigo)
        .build());
  }

  @PostMapping(value = "/simplificado/{empresaId}/{sucursalId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
  @Operation(summary = "Crear producto para régimen simplificado",
      description = "Crea un producto validando el régimen tributario de la sucursal")
  public ResponseEntity<ApiResponse<ProductoDto>> crearProductoSimplificado(
      @PathVariable Long empresaId,
      @PathVariable Long sucursalId,
      @RequestPart("producto") @Valid ProductoCreateDto productoDto,
      @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

    log.info("POST /api/productos/simplificado/{}/{} - Creando producto régimen simplificado con imagen: {}",
        empresaId, sucursalId, imagen != null ? imagen.getOriginalFilename() : "sin imagen");

    try {
      ProductoDto productoCreado = productoCrudService.crearProductoSimplificado(
          empresaId, sucursalId, productoDto, imagen);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.<ProductoDto>builder()
              .success(true)
              .message("Producto creado exitosamente")
              .data(productoCreado)
              .build());

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error creando producto simplificado", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al crear el producto: " + e.getMessage()));
    }
  }

  /**
   * Helper para obtener información de facturación de la sucursal
   */
  @GetMapping("/sucursal/{sucursalId}/modo-facturacion")
  @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
  @Operation(summary = "Obtener modo de facturación de sucursal")
  public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerModoFacturacion(
      @PathVariable Long sucursalId) {

    try {
      Map<String, Object> modoInfo = productoCrudService.obtenerModoFacturacion(sucursalId);

      return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
          .success(true)
          .message("Información de facturación obtenida")
          .data(modoInfo)
          .build());
    } catch (Exception e) {
      log.error("Error obteniendo modo de facturación", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Error al obtener información de facturación"));
    }
  }

  @Data
  @Builder
  public static class ModoFacturacionResponse {
    private Long sucursalId;
    private Long empresaId;
    private boolean requiereFacturaElectronica;
    private ModoFacturacion modoFacturacion;
    private RegimenTributario regimenTributario;
  }
}