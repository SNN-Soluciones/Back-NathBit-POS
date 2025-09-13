package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.categoria.CategoriaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.categoria.CategoriaProductoResponse;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.service.CategoriaProductoService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorias-producto")
@RequiredArgsConstructor
@Tag(name = "Categorías de Productos", description = "Gestión de categorías de productos")
@Slf4j
public class CategoriaProductoController {

    private final CategoriaProductoService categoriaService;
    private final EmpresaService empresaService;

    @Operation(summary = "Listar categorías por empresa")
    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<List<CategoriaProductoResponse>>> listarPorEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String busqueda) {
        
        log.info("Listando categorías de empresa: {}", empresaId);
        
        List<CategoriaProducto> categorias = categoriaService.listarPorEmpresa(empresaId, busqueda);
        List<CategoriaProductoResponse> response = categorias.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok(
                "Categorías encontradas: " + response.size(), 
                response
        ));
    }

    @Operation(summary = "Obtener categoría por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> obtenerPorId(@PathVariable Long id) {
        
        CategoriaProducto categoria = categoriaService.buscarPorId(id).orElse(null);
        if (categoria == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Categoría no encontrada"));
        }
        
        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(categoria)));
    }

    @Operation(summary = "Crear nueva categoría")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> crear(
            @Valid @RequestBody CategoriaProductoRequest request) {
        
        log.info("Creando categoría: {} para empresa: {}", request.getNombre(), request.getEmpresaId());
        
        try {
            // Buscar empresa
            Empresa empresa = empresaService.buscarPorId(request.getEmpresaId());
            if (empresa == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Empresa no encontrada"));
            }
            
            // Verificar si ya existe
            if (categoriaService.existePorNombreYEmpresa(request.getNombre(), request.getEmpresaId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Ya existe una categoría con ese nombre"));
            }
            
            // Crear entidad
            CategoriaProducto categoria = CategoriaProducto.builder()
                    .empresa(empresa)
                    .nombre(request.getNombre())
                    .descripcion(request.getDescripcion())
                    .color(request.getColor())
                    .icono(request.getIcono())
                    .orden(request.getOrden() != null ? request.getOrden() : 0)
                    .activo(true)
                    .build();
            
            CategoriaProducto categoriaNueva = categoriaService.crear(categoria);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Categoría creada exitosamente", 
                            convertirAResponse(categoriaNueva)));
                            
        } catch (Exception e) {
            log.error("Error al crear categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al crear categoría: " + e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar categoría")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaProductoRequest request) {
        
        log.info("Actualizando categoría ID: {}", id);
        
        try {
            CategoriaProducto categoriaExistente = categoriaService.buscarPorId(id).orElse(null);
            if (categoriaExistente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Categoría no encontrada"));
            }
            
            // Verificar nombre duplicado (si cambió)
            if (!categoriaExistente.getNombre().equals(request.getNombre())) {
                if (categoriaService.existePorNombreYEmpresa(request.getNombre(), 
                        categoriaExistente.getEmpresa().getId())) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Ya existe una categoría con ese nombre"));
                }
            }
            
            // Actualizar campos
            categoriaExistente.setNombre(request.getNombre());
            categoriaExistente.setDescripcion(request.getDescripcion());
            categoriaExistente.setColor(request.getColor());
            categoriaExistente.setIcono(request.getIcono());
            if (request.getOrden() != null) {
                categoriaExistente.setOrden(request.getOrden());
            }
            
            CategoriaProducto categoriaActualizada = categoriaService.actualizar(id, categoriaExistente);
            
            return ResponseEntity.ok(ApiResponse.ok(
                    "Categoría actualizada exitosamente",
                    convertirAResponse(categoriaActualizada)
            ));
            
        } catch (Exception e) {
            log.error("Error al actualizar categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al actualizar categoría: " + e.getMessage()));
        }
    }

    @Operation(summary = "Activar/Desactivar categoría")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> toggleActivo(@PathVariable Long id) {
        
        log.info("Cambiando estado de categoría ID: {}", id);
        
        try {
            CategoriaProducto categoria = categoriaService.buscarPorId(id).orElse(null);
            if (categoria == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Categoría no encontrada"));
            }
            
            // Verificar si tiene productos activos antes de desactivar
            if (categoria.getActivo() && categoriaService.tieneProductosActivos(id)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No se puede desactivar una categoría con productos activos"));
            }
            
            categoria.setActivo(!categoria.getActivo());
            CategoriaProducto categoriaActualizada = categoriaService.actualizar(id, categoria);
            
            String mensaje = categoriaActualizada.getActivo() ? 
                    "Categoría activada exitosamente" : 
                    "Categoría desactivada exitosamente";
            
            return ResponseEntity.ok(ApiResponse.ok(mensaje, 
                    convertirAResponse(categoriaActualizada)));
                    
        } catch (Exception e) {
            log.error("Error al cambiar estado de categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al cambiar estado: " + e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar categoría (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> eliminar(@PathVariable Long id) {
        
        log.info("Eliminando categoría ID: {}", id);
        
        try {
            CategoriaProducto categoria = categoriaService.buscarPorId(id).orElse(null);
            if (categoria == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Categoría no encontrada"));
            }
            
            // Verificar si tiene productos
            if (categoriaService.tieneProductosActivos(id)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No se puede eliminar una categoría con productos asociados"));
            }
            
            // Soft delete - solo desactivar
            categoria.setActivo(false);
            categoriaService.actualizar(id, categoria);
            
            return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada exitosamente"));
            
        } catch (Exception e) {
            log.error("Error al eliminar categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al eliminar categoría: " + e.getMessage()));
        }
    }

    // Método auxiliar para convertir entidad a response
    private CategoriaProductoResponse convertirAResponse(CategoriaProducto categoria) {
        return CategoriaProductoResponse.builder()
                .id(categoria.getId())
                .empresaId(categoria.getEmpresa().getId())
                .empresaNombre(categoria.getEmpresa().getNombreComercial())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .color(categoria.getColor())
                .icono(categoria.getIcono())
                .orden(categoria.getOrden())
                .activo(categoria.getActivo())
                .cantidadProductos(categoriaService.contarProductosActivos(categoria.getId()))
                .createdAt(categoria.getCreatedAt())
                .updatedAt(categoria.getUpdatedAt())
                .build();
    }
}