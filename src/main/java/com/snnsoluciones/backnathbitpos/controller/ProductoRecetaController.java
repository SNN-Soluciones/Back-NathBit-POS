package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.DescontarIngredientesDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaDto;
import com.snnsoluciones.backnathbitpos.dto.producto.RecetaUpdateDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.VerificarProduccionDTO;
import com.snnsoluciones.backnathbitpos.entity.ProductoReceta;
import com.snnsoluciones.backnathbitpos.service.ProductoRecetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/recetas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductoRecetaController {

    private final ProductoRecetaService recetaService;

    // Crear receta
    @PostMapping
    public ResponseEntity<ApiResponse<ProductoReceta>> crearReceta(@Valid @RequestBody RecetaCreateDTO dto) {
        try {
            ProductoReceta receta = recetaService.crearReceta(dto.getEmpresaId(), dto.getProductoId(), dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Receta creada exitosamente", receta));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al crear receta: " + e.getMessage()));
        }
    }

    // Actualizar receta
    @PutMapping("/{recetaId}")
    public ResponseEntity<ApiResponse<ProductoReceta>> actualizarReceta(
        @PathVariable Long recetaId,
        @Valid @RequestBody RecetaUpdateDTO dto) {
        try {
            ProductoReceta receta = recetaService.actualizarReceta(
                dto.getEmpresaId(),
                recetaId,
                dto);
            return ResponseEntity.ok(ApiResponse.ok("Receta actualizada", receta));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al actualizar receta: " + e.getMessage()));
        }
    }

    // Obtener receta de un producto
    @GetMapping("/empresa/{empresaId}/producto/{productoId}")
    public ResponseEntity<ApiResponse<RecetaDto>> obtenerReceta(
        @PathVariable Long empresaId,
        @PathVariable Long productoId) {
        try {
            RecetaDto receta = recetaService.obtenerReceta(empresaId, productoId);
            return ResponseEntity.ok(ApiResponse.ok("Receta encontrada", receta));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Receta no encontrada"));
        }
    }

    // Listar recetas de una empresa
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<ApiResponse<List<RecetaDto>>> listarRecetas(@PathVariable Long empresaId) {
        try {
            List<RecetaDto> recetas = recetaService.listarRecetas(empresaId);
            return ResponseEntity.ok(ApiResponse.ok("Recetas obtenidas", recetas));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al obtener recetas: " + e.getMessage()));
        }
    }

    // Verificar si se puede producir
    @PostMapping("/verificar-produccion")
    public ResponseEntity<ApiResponse<Boolean>> verificarProduccion(@Valid @RequestBody VerificarProduccionDTO dto) {
        try {
            boolean puedeProducir = recetaService.puedeProducir(
                dto.getEmpresaId(),
                dto.getProductoId(),
                dto.getSucursalId(),
                dto.getCantidad());

            String mensaje = puedeProducir ?
                "Se puede producir la cantidad solicitada" :
                "Stock insuficiente de ingredientes";

            return ResponseEntity.ok(ApiResponse.ok(mensaje, puedeProducir));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error al verificar producción: " + e.getMessage()));
        }
    }

    // Descontar ingredientes (usado internamente por ventas)
    @PostMapping("/descontar-ingredientes")
    public ResponseEntity<ApiResponse<Void>> descontarIngredientes(@Valid @RequestBody DescontarIngredientesDTO dto) {
        try {
            recetaService.descontarIngredientes(
                dto.getEmpresaId(),
                dto.getProductoId(),
                dto.getSucursalId(),
                dto.getCantidad());

            return ResponseEntity.ok(ApiResponse.success("Ingredientes descontados", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al descontar ingredientes: " + e.getMessage()));
        }
    }

//    // Eliminar receta (soft delete)
//    @DeleteMapping("/{recetaId}")
//    public ResponseEntity<ApiResponse<Void>> eliminarReceta(
//        @PathVariable Long recetaId,
//        @RequestParam Long empresaId) {
//        try {
//            recetaService.eliminarReceta(empresaId, recetaId);
//            return ResponseEntity.ok(ApiResponse.success("Receta eliminada", null));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error("Error al eliminar receta: " + e.getMessage()));
//        }
//    }
}