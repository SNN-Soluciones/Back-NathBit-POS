package com.snnsoluciones.backnathbitpos.controller.producto;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import com.snnsoluciones.backnathbitpos.service.producto.ProductoService;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Controller unificado para gestión de productos.
 * Reemplaza a ProductoController y ProductoControllerV2 antiguos.
 */
@Slf4j
@RestController
@RequestMapping("/api/v3/productos")
@RequiredArgsConstructor
@Tag(name = "Productos V3", description = "API unificada para gestión de productos")
public class ProductoControllerV3 {

    private final ProductoService productoService;

    // ==================== CRUD BÁSICO ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto con imagen opcional")
    public ResponseEntity<ApiResponse<ProductoDto>> crear(
            @RequestPart("producto") @Valid ProductoCreateDto dto,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

        log.info("POST /api/v3/productos - Creando producto: {} para empresa: {}", 
            dto.getNombre(), dto.getEmpresaId());

        try {
            ProductoDto producto = productoService.crear(dto, imagen);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductoDto>builder()
                    .success(true)
                    .message("Producto creado exitosamente")
                    .data(producto)
                    .build());

        } catch (Exception e) {
            log.error("Error creando producto", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Actualiza un producto con imagen opcional")
    public ResponseEntity<ApiResponse<ProductoDto>> actualizar(
            @PathVariable Long id,
            @RequestPart("producto") @Valid ProductoUpdateDto dto,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

        log.info("PUT /api/v3/productos/{} - Actualizando producto", id);

        try {
            ProductoDto producto = productoService.actualizar(id, dto, imagen);

            return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
                .success(true)
                .message("Producto actualizado exitosamente")
                .data(producto)
                .build());

        } catch (Exception e) {
            log.error("Error actualizando producto {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Obtener producto por ID", description = "Obtiene los detalles de un producto")
    public ResponseEntity<ApiResponse<ProductoDto>> obtenerPorId(@PathVariable Long id) {

        log.debug("GET /api/v3/productos/{}", id);

        try {
            ProductoDto producto = productoService.obtenerPorId(id);

            return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
                .success(true)
                .data(producto)
                .build());

        } catch (Exception e) {
            log.error("Error obteniendo producto {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Eliminar producto", description = "Elimina un producto permanentemente")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {

        log.info("DELETE /api/v3/productos/{}", id);

        try {
            productoService.eliminar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto eliminado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error eliminando producto {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activar producto", description = "Activa un producto desactivado")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable Long id) {

        log.info("PATCH /api/v3/productos/{}/activar", id);

        try {
            productoService.activar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto activado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error activando producto {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Desactivar producto", description = "Desactiva un producto sin eliminarlo")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Long id) {

        log.info("PATCH /api/v3/productos/{}/desactivar", id);

        try {
            productoService.desactivar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto desactivado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error desactivando producto {}", id, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== CONSULTAS Y BÚSQUEDAS ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Listar productos", description = "Lista productos de una empresa con paginación")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> listar(
            @RequestParam Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("GET /api/v3/productos - empresaId: {}, page: {}, size: {}", empresaId, page, size);

        try {
            Sort sort = sortDir.equalsIgnoreCase("DESC") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ProductoDto> productos = productoService.listar(empresaId, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error listando productos", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Listar productos activos", description = "Lista solo productos activos")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> listarActivos(
            @RequestParam Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v3/productos/activos - empresaId: {}", empresaId);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
            Page<ProductoDto> productos = productoService.listarActivos(empresaId, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error listando productos activos", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Listar productos por sucursal", description = "Lista productos de una sucursal con paginación")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> listarPorSucursal(
        @PathVariable Long sucursalId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "nombre") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir) {

        log.debug("GET /api/v3/productos/sucursal/{} - page: {}, size: {}", sucursalId, page, size);

        try {
            Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ProductoDto> productos = productoService.buscarPorSucursal(sucursalId, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error listando productos de sucursal {}", sucursalId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/sucursal/{sucursalId}/buscar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar productos en sucursal", description = "Busca productos en una sucursal por término")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> buscarEnSucursal(
        @PathVariable Long sucursalId,
        @RequestParam String termino,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v3/productos/sucursal/{}/buscar - termino: {}", sucursalId, termino);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductoDto> productos = productoService.buscarPorSucursal(sucursalId, termino, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error buscando productos en sucursal {}", sucursalId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar productos", description = "Busca productos por término (código, nombre)")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> buscar(
            @RequestParam Long empresaId,
            @RequestParam String termino,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v3/productos/buscar - empresaId: {}, termino: {}", empresaId, termino);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductoDto> productos = productoService.buscar(empresaId, termino, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error buscando productos", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/codigo/{codigoInterno}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar por código", description = "Busca un producto por código interno")
    public ResponseEntity<ApiResponse<ProductoDto>> buscarPorCodigo(
            @PathVariable String codigoInterno,
            @RequestParam Long empresaId) {

        log.debug("GET /api/v3/productos/codigo/{} - empresaId: {}", codigoInterno, empresaId);

        try {
            ProductoDto producto = productoService.obtenerPorCodigo(codigoInterno, empresaId);

            return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
                .success(true)
                .data(producto)
                .build());

        } catch (Exception e) {
            log.error("Error buscando producto por código", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/familia/{familiaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(summary = "Buscar por familia", description = "Lista productos de una familia")
    public ResponseEntity<ApiResponse<Page<ProductoDto>>> buscarPorFamilia(
            @PathVariable Long familiaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v3/productos/familia/{}", familiaId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductoDto> productos = productoService.buscarPorFamilia(familiaId, pageable);

            return ResponseEntity.ok(ApiResponse.<Page<ProductoDto>>builder()
                .success(true)
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error buscando productos por familia", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoDto>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== UTILIDADES ====================

    @GetMapping("/generar-codigo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Generar código", description = "Genera un código interno único")
    public ResponseEntity<ApiResponse<String>> generarCodigo(@RequestParam Long empresaId) {

        log.debug("GET /api/v3/productos/generar-codigo - empresaId: {}", empresaId);

        try {
            String codigo = productoService.generarCodigoInterno(empresaId);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(codigo)
                .build());

        } catch (Exception e) {
            log.error("Error generando código", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/existe-codigo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Verificar código", description = "Verifica si existe un código interno")
    public ResponseEntity<ApiResponse<Boolean>> existeCodigo(
            @RequestParam String codigo,
            @RequestParam Long empresaId) {

        log.debug("GET /api/v3/productos/existe-codigo - codigo: {}, empresaId: {}", codigo, empresaId);

        try {
            boolean existe = productoService.existeCodigo(codigo, empresaId);

            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .data(existe)
                .build());

        } catch (Exception e) {
            log.error("Error verificando código", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PatchMapping("/{id}/precio")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar precio", description = "Actualiza solo el precio de un producto")
    public ResponseEntity<ApiResponse<Void>> actualizarPrecio(
            @PathVariable Long id,
            @RequestParam BigDecimal precio) {

        log.info("PATCH /api/v3/productos/{}/precio - nuevoPrecio: {}", id, precio);

        try {
            productoService.actualizarPrecio(id, precio);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Precio actualizado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error actualizando precio", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== IMÁGENES ====================

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Actualizar imagen", description = "Actualiza solo la imagen de un producto")
    public ResponseEntity<ApiResponse<Void>> actualizarImagen(
            @PathVariable Long id,
            @RequestPart("imagen") MultipartFile imagen) {

        log.info("POST /api/v3/productos/{}/imagen", id);

        try {
            productoService.actualizarImagen(id, imagen);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Imagen actualizada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error actualizando imagen", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}/imagen")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Eliminar imagen", description = "Elimina la imagen de un producto")
    public ResponseEntity<ApiResponse<Void>> eliminarImagen(@PathVariable Long id) {

        log.info("DELETE /api/v3/productos/{}/imagen", id);

        try {
            productoService.eliminarImagen(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Imagen eliminada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error eliminando imagen", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== CATEGORÍAS ====================

    @PutMapping("/{id}/categorias")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Asignar categorías", description = "Asigna categorías a un producto (reemplaza existentes)")
    public ResponseEntity<ApiResponse<Void>> asignarCategorias(
            @PathVariable Long id,
            @RequestBody Set<Long> categoriaIds) {

        log.info("PUT /api/v3/productos/{}/categorias - {} categorías", id, categoriaIds.size());

        try {
            productoService.asignarCategorias(id, categoriaIds);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Categorías asignadas exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error asignando categorías", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Agregar categoría", description = "Agrega una categoría sin eliminar las existentes")
    public ResponseEntity<ApiResponse<Void>> agregarCategoria(
            @PathVariable Long id,
            @PathVariable Long categoriaId) {

        log.info("POST /api/v3/productos/{}/categorias/{}", id, categoriaId);

        try {
            productoService.agregarCategoria(id, categoriaId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Categoría agregada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error agregando categoría", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}/categorias/{categoriaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Quitar categoría", description = "Quita una categoría específica")
    public ResponseEntity<ApiResponse<Void>> quitarCategoria(
            @PathVariable Long id,
            @PathVariable Long categoriaId) {

        log.info("DELETE /api/v3/productos/{}/categorias/{}", id, categoriaId);

        try {
            productoService.quitarCategoria(id, categoriaId);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Categoría quitada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error quitando categoría", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }
}