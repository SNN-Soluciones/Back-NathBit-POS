package com.snnsoluciones.backnathbitpos.controller.producto;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoListDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import com.snnsoluciones.backnathbitpos.service.producto.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/**
 * Controller V3 unificado para gestión de productos.
 * 
 * CARACTERÍSTICAS:
 * - Soporta productos GLOBALES (empresaId) y LOCALES (empresaId + sucursalId)
 * - Endpoints REST limpios sin empresaId en path
 * - Paginación configurable (default: 15 elementos)
 * - Manejo completo de imágenes
 * 
 * DEPRECA:
 * - ProductoController (V1)
 * - ProductoControllerV2
 */
@Slf4j
@RestController
@RequestMapping("/api/v3/productos")
@RequiredArgsConstructor
@Tag(name = "Productos V3", description = "API unificada para gestión de productos (globales y locales)")
public class ProductoControllerV3 {

    private final ProductoService productoService;

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final String DEFAULT_SORT_BY = "nombre";

    // ==================== CREAR PRODUCTO ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Crear producto",
        description = """
            Crea un nuevo producto con imagen opcional.
            
            ESTRATEGIA DE CREACIÓN:
            - Si sucursalId es NULL → Producto GLOBAL (disponible en toda la empresa)
            - Si sucursalId tiene valor → Producto LOCAL (solo esa sucursal)
            
            El código interno se genera automáticamente si no se proporciona.
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Producto creado exitosamente",
            content = @Content(schema = @Schema(implementation = ProductoDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o código duplicado"
        )
    })
    public ResponseEntity<ApiResponse<ProductoDto>> crear(
            @Parameter(description = "Datos del producto a crear", required = true)
            @RequestPart("producto") @Valid ProductoCreateDto dto,
            
            @Parameter(description = "Imagen del producto (opcional, JPG/PNG/WEBP, max 5MB)")
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {

        log.info("POST /api/v3/productos - Creando producto: {} para empresa: {}, sucursal: {}", 
            dto.getNombre(), dto.getEmpresaId(), dto.getSucursalId());

        try {
            ProductoDto producto = productoService.crear(dto, imagen);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductoDto>builder()
                    .success(true)
                    .message(producto.getSucursalId() == null 
                        ? "Producto GLOBAL creado exitosamente" 
                        : "Producto LOCAL creado exitosamente")
                    .data(producto)
                    .build());

        } catch (Exception e) {
            log.error("Error creando producto: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error al crear producto: " + e.getMessage())
                    .build());
        }
    }

    // ==================== ACTUALIZAR PRODUCTO ====================

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Actualizar producto",
        description = """
            Actualiza un producto existente con imagen opcional.
            
            REGLAS:
            - NO se puede cambiar empresaId
            - NO se puede cambiar sucursalId (global ↔ local no se puede cambiar)
            - NO se puede cambiar codigoInterno
            - Si viene imagen nueva, reemplaza la anterior
            - Solo se actualizan los campos que vienen en el request
            """
    )
    public ResponseEntity<ApiResponse<ProductoDto>> actualizar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id,
            
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestPart("producto") @Valid ProductoUpdateDto dto,
            
            @Parameter(description = "Nueva imagen (opcional)")
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
            log.error("Error actualizando producto ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error al actualizar producto: " + e.getMessage())
                    .build());
        }
    }

    // ==================== OBTENER UN PRODUCTO ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(
        summary = "Obtener producto por ID",
        description = "Devuelve un producto completo con todas sus relaciones (categorías, impuestos, familia)"
    )
    public ResponseEntity<ApiResponse<ProductoDto>> obtenerPorId(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {

        log.debug("GET /api/v3/productos/{}", id);

        try {
            ProductoDto producto = productoService.obtenerPorId(id);

            return ResponseEntity.ok(ApiResponse.<ProductoDto>builder()
                .success(true)
                .data(producto)
                .build());

        } catch (Exception e) {
            log.error("Error obteniendo producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<ProductoDto>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== LISTAR PRODUCTOS ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO', 'MESERO')")
    @Operation(
        summary = "Listar productos con paginación y filtros",
        description = """
        Lista productos según los parámetros proporcionados.
        
        ESTRATEGIA DE CONSULTA:
        - Solo empresaId → Devuelve SOLO productos GLOBALES (sucursalId = NULL)
        - empresaId + sucursalId → Devuelve productos GLOBALES + LOCALES de esa sucursal
        - termino → Busca en código interno, código barras, nombre y descripción
        - activo → Filtra solo productos activos
        - tipo → Filtra por tipo de producto (VENTA, MATERIA_PRIMA, COMPUESTO, COMBO, RECETA) ✨ NUEVO
        
        PAGINACIÓN:
        - page: Número de página (inicia en 0)
        - size: Elementos por página (default: 15)
        - sortBy: Campo para ordenar (default: nombre)
        - sortDir: Dirección (asc/desc, default: asc)
        """
    )
    public ResponseEntity<ApiResponse<Page<ProductoListDto>>> listar(
        @Parameter(description = "ID de la empresa", required = true)
        @RequestParam Long empresaId,

        @Parameter(description = "ID de sucursal (opcional, NULL = solo globales)")
        @RequestParam(required = false) Long sucursalId,

        @Parameter(description = "Término de búsqueda (opcional)")
        @RequestParam(required = false) String termino,

        @Parameter(description = "Filtrar solo activos (default: false)")
        @RequestParam(defaultValue = "false") boolean activo,

        @Parameter(description = "Tipo de producto (opcional): VENTA, MATERIA_PRIMA, COMPUESTO, COMBO, RECETA") // ✨ NUEVO
        @RequestParam(required = false) String tipo, // ✨ NUEVO

        @Parameter(description = "Número de página (inicia en 0)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Elementos por página")
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,

        @Parameter(description = "Campo para ordenar")
        @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,

        @Parameter(description = "Dirección de ordenamiento")
        @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("GET /api/v3/productos - empresaId={}, sucursalId={}, termino={}, activo={}, tipo={}",
            empresaId, sucursalId, termino, activo, tipo); // ✨ AGREGADO tipo al log

        try {
            // Crear Pageable
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // Determinar qué método usar
            Page<ProductoListDto> productos;

            if (termino != null && !termino.trim().isEmpty()) {
                // BÚSQUEDA con término (puede combinarse con tipo)
                productos = productoService.buscar(empresaId, sucursalId, termino, tipo, activo, pageable); // ✨ AGREGADO tipo
            } else if (activo) {
                // LISTAR solo activos (puede combinarse con tipo)
                productos = productoService.listarActivos(empresaId, sucursalId, tipo, pageable); // ✨ AGREGADO tipo
            } else {
                // LISTAR todos (puede combinarse con tipo)
                productos = productoService.listar(empresaId, sucursalId, tipo, pageable); // ✨ AGREGADO tipo
            }

            return ResponseEntity.ok(ApiResponse.<Page<ProductoListDto>>builder()
                .success(true)
                .message(String.format("Se encontraron %d productos", productos.getTotalElements()))
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error listando productos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoListDto>>builder()
                    .success(false)
                    .message("Error al listar productos: " + e.getMessage())
                    .build());
        }
    }

    // ==================== ELIMINAR PRODUCTO ====================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Eliminar producto",
        description = "Elimina un producto (borrado lógico: activo = false)"
    )
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {

        log.info("DELETE /api/v3/productos/{}", id);

        try {
            productoService.eliminar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto eliminado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error eliminando producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar producto: " + e.getMessage())
                    .build());
        }
    }

    // ==================== ACTIVAR/DESACTIVAR ====================

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activar producto", description = "Cambia el estado del producto a activo")
    public ResponseEntity<ApiResponse<Void>> activar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {

        log.info("PATCH /api/v3/productos/{}/activar", id);

        try {
            productoService.activar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto activado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error activando producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Desactivar producto", description = "Cambia el estado del producto a inactivo")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {

        log.info("PATCH /api/v3/productos/{}/desactivar", id);

        try {
            productoService.desactivar(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Producto desactivado exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error desactivando producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    // ==================== GESTIÓN DE IMÁGENES ====================

    @PatchMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Actualizar solo la imagen",
        description = "Actualiza únicamente la imagen del producto (elimina la anterior si existe)"
    )
    public ResponseEntity<ApiResponse<Void>> actualizarImagen(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id,
            
            @Parameter(description = "Nueva imagen (JPG/PNG/WEBP, max 5MB)", required = true)
            @RequestPart("imagen") MultipartFile imagen) {

        log.info("PATCH /api/v3/productos/{}/imagen", id);

        try {
            productoService.actualizarImagen(id, imagen);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Imagen actualizada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error actualizando imagen producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al actualizar imagen: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}/imagen")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Eliminar imagen",
        description = "Elimina la imagen del producto de DigitalOcean Spaces"
    )
    public ResponseEntity<ApiResponse<Void>> eliminarImagen(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Long id) {

        log.info("DELETE /api/v3/productos/{}/imagen", id);

        try {
            productoService.eliminarImagen(id);

            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Imagen eliminada exitosamente")
                .build());

        } catch (Exception e) {
            log.error("Error eliminando imagen producto ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al eliminar imagen: " + e.getMessage())
                    .build());
        }
    }

    // ==================== UTILIDADES ====================

    @GetMapping("/generar-codigo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Generar código interno",
        description = "Genera un código interno único para la empresa (formato: PROD-00001)"
    )
    public ResponseEntity<ApiResponse<String>> generarCodigo(
            @Parameter(description = "ID de la empresa", required = true)
            @RequestParam Long empresaId) {

        log.debug("GET /api/v3/productos/generar-codigo?empresaId={}", empresaId);

        try {
            String codigo = productoService.generarCodigoInterno(empresaId);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Código generado")
                .data(codigo)
                .build());

        } catch (Exception e) {
            log.error("Error generando código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/validar-codigo")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN')")
    @Operation(
        summary = "Validar si un código interno existe",
        description = "Verifica si un código interno ya está en uso en la empresa"
    )
    public ResponseEntity<ApiResponse<Boolean>> validarCodigo(
            @Parameter(description = "Código interno a validar", required = true)
            @RequestParam String codigoInterno,
            
            @Parameter(description = "ID de la empresa", required = true)
            @RequestParam Long empresaId) {

        log.debug("GET /api/v3/productos/validar-codigo?codigo={}&empresaId={}", 
            codigoInterno, empresaId);

        try {
            boolean existe = productoService.existeCodigoInterno(codigoInterno, empresaId);

            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message(existe ? "El código ya existe" : "El código está disponible")
                .data(existe)
                .build());

        } catch (Exception e) {
            log.error("Error validando código: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }


    /**
     * Buscar productos por categoría
     */
    @GetMapping("/categoria/{categoriaId}")
    @Operation(
        summary = "Listar productos por categoría",
        description = "Lista productos de una categoría específica con filtros de empresa/sucursal"
    )
    public ResponseEntity<ApiResponse<Page<ProductoListDto>>> listarPorCategoria(
        @PathVariable Long categoriaId,
        @RequestParam Long empresaId,
        @RequestParam(required = false) Long sucursalId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "nombre") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("Listando productos por categoría: {}, empresa: {}, sucursal: {}",
            categoriaId, empresaId, sucursalId);

        try {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<ProductoListDto> productos = productoService.buscarPorCategoria(
                categoriaId,
                empresaId,
                sucursalId,
                pageable
            );

            return ResponseEntity.ok(ApiResponse.<Page<ProductoListDto>>builder()
                .success(true)
                .message("Productos de la categoría")
                .data(productos)
                .build());

        } catch (Exception e) {
            log.error("Error listando productos por categoría: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Page<ProductoListDto>>builder()
                    .success(false)
                    .message("Error al listar productos: " + e.getMessage())
                    .build());
        }
    }
}