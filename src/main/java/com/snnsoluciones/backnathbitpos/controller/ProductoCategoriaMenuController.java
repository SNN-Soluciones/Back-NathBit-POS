// ProductoCategoriaMenuController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.producto.ActualizarCategoriaMenuItemRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.CategoriaMenuItemResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.CrearCategoriaMenuItemRequest;
import com.snnsoluciones.backnathbitpos.service.ProductoCategoriaMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/productos/categoria-menu")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categoría de Menú", description = "Gestión de categorías de menú y sus items")
public class ProductoCategoriaMenuController {

    private final ProductoCategoriaMenuService categoriaMenuService;

    @GetMapping("/{categoriaMenuId}/items")
    @Operation(summary = "Obtener items de una categoría de menú")
    public ResponseEntity<List<CategoriaMenuItemResponse>> obtenerItems(
        @PathVariable Long categoriaMenuId
    ) {
        log.info("GET /api/productos/categoria-menu/{}/items", categoriaMenuId);
        List<CategoriaMenuItemResponse> items = categoriaMenuService.obtenerItemsPorCategoria(categoriaMenuId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{categoriaMenuId}/items")
    @Operation(summary = "Agregar un producto a la categoría de menú")
    public ResponseEntity<CategoriaMenuItemResponse> agregarItem(
        @PathVariable Long categoriaMenuId,
        @Valid @RequestBody CrearCategoriaMenuItemRequest request
    ) {
        log.info("POST /api/productos/categoria-menu/{}/items - Body: {}", categoriaMenuId, request);
        CategoriaMenuItemResponse item = categoriaMenuService.agregarItemACategoria(categoriaMenuId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Actualizar un item de categoría")
    public ResponseEntity<CategoriaMenuItemResponse> actualizarItem(
        @PathVariable Long itemId,
        @Valid @RequestBody ActualizarCategoriaMenuItemRequest request
    ) {
        log.info("PUT /api/productos/categoria-menu/items/{} - Body: {}", itemId, request);
        CategoriaMenuItemResponse item = categoriaMenuService.actualizarItem(itemId, request);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Eliminar un item de categoría")
    public ResponseEntity<Void> eliminarItem(@PathVariable Long itemId) {
        log.info("DELETE /api/productos/categoria-menu/items/{}", itemId);
        categoriaMenuService.eliminarItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoriaMenuId}/items/reordenar")
    @Operation(summary = "Reordenar items de una categoría")
    public ResponseEntity<List<CategoriaMenuItemResponse>> reordenarItems(
        @PathVariable Long categoriaMenuId,
        @RequestBody List<Long> itemIdsOrdenados
    ) {
        log.info("PUT /api/productos/categoria-menu/{}/items/reordenar - IDs: {}", categoriaMenuId, itemIdsOrdenados);
        List<CategoriaMenuItemResponse> items = categoriaMenuService.reordenarItems(categoriaMenuId, itemIdsOrdenados);
        return ResponseEntity.ok(items);
    }
}
