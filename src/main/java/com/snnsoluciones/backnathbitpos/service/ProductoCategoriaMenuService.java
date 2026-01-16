// ProductoCategoriaMenuService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ActualizarCategoriaMenuItemRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.CategoriaMenuItemResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.CrearCategoriaMenuItemRequest;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoCategoriaMenuItem;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoCategoriaMenuItemRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoCategoriaMenuService {

    private final ProductoCategoriaMenuItemRepository categoriaMenuItemRepository;
    private final ProductoRepository productoRepository;

    /**
     * Obtener todos los items de una categoría de menú
     */
    @Transactional(readOnly = true)
    public List<CategoriaMenuItemResponse> obtenerItemsPorCategoria(Long categoriaMenuId) {
        log.info("Obteniendo items de categoría de menú ID: {}", categoriaMenuId);
        
        // Validar que la categoría exista y sea del tipo correcto
        validarCategoriaMenu(categoriaMenuId);
        
        List<ProductoCategoriaMenuItem> items = categoriaMenuItemRepository
            .findByCategoriaMenuIdOrderByOrdenAsc(categoriaMenuId);
        
        return items.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());
    }

    /**
     * Agregar un producto a una categoría de menú
     */
    @Transactional
    public CategoriaMenuItemResponse agregarItemACategoria(
        Long categoriaMenuId, 
        CrearCategoriaMenuItemRequest request
    ) {
        log.info("Agregando producto {} a categoría {}", request.getProductoHijoId(), categoriaMenuId);
        
        // Validaciones
        validarCategoriaMenu(categoriaMenuId);
        validarProductoHijo(request.getProductoHijoId());
        
        // Verificar que no exista duplicado
        if (categoriaMenuItemRepository.existsByCategoriaMenuIdAndProductoHijoId(
            categoriaMenuId, request.getProductoHijoId())) {
            throw new BadRequestException("El producto ya está asignado a esta categoría");
        }
        
        // Crear item
        ProductoCategoriaMenuItem item = ProductoCategoriaMenuItem.builder()
            .categoriaMenuId(categoriaMenuId)
            .productoHijoId(request.getProductoHijoId())
            .orden(request.getOrden())
            .destacado(request.getDestacado() != null ? request.getDestacado() : false)
            .build();
        
        ProductoCategoriaMenuItem itemGuardado = categoriaMenuItemRepository.save(item);
        
        return convertirAResponse(itemGuardado);
    }

    /**
     * Actualizar un item de categoría
     */
    @Transactional
    public CategoriaMenuItemResponse actualizarItem(
        Long itemId, 
        ActualizarCategoriaMenuItemRequest request
    ) {
        log.info("Actualizando item de categoría ID: {}", itemId);
        
        ProductoCategoriaMenuItem item = categoriaMenuItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Item de categoría no encontrado"));
        
        if (request.getOrden() != null) {
            item.setOrden(request.getOrden());
        }
        
        if (request.getDestacado() != null) {
            item.setDestacado(request.getDestacado());
        }
        
        ProductoCategoriaMenuItem itemActualizado = categoriaMenuItemRepository.save(item);
        
        return convertirAResponse(itemActualizado);
    }

    /**
     * Eliminar un item de categoría
     */
    @Transactional
    public void eliminarItem(Long itemId) {
        log.info("Eliminando item de categoría ID: {}", itemId);
        
        if (!categoriaMenuItemRepository.existsById(itemId)) {
            throw new NotFoundException("Item de categoría no encontrado");
        }
        
        categoriaMenuItemRepository.deleteById(itemId);
    }

    /**
     * Reordenar items de una categoría
     */
    @Transactional
    public List<CategoriaMenuItemResponse> reordenarItems(
        Long categoriaMenuId, 
        List<Long> itemIdsOrdenados
    ) {
        log.info("Reordenando items de categoría ID: {}", categoriaMenuId);
        
        validarCategoriaMenu(categoriaMenuId);
        
        List<ProductoCategoriaMenuItem> items = categoriaMenuItemRepository
            .findByCategoriaMenuIdOrderByOrdenAsc(categoriaMenuId);
        
        // Validar que los IDs coincidan
        if (items.size() != itemIdsOrdenados.size()) {
            throw new BadRequestException("La cantidad de items no coincide");
        }
        
        // Actualizar orden
        for (int i = 0; i < itemIdsOrdenados.size(); i++) {
            Long itemId = itemIdsOrdenados.get(i);
            ProductoCategoriaMenuItem item = items.stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Item ID " + itemId + " no encontrado"));
            
            item.setOrden(i);
        }
        
        List<ProductoCategoriaMenuItem> itemsActualizados = categoriaMenuItemRepository.saveAll(items);
        
        return itemsActualizados.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());
    }

    // ==================== VALIDACIONES ====================

    private void validarCategoriaMenu(Long categoriaMenuId) {
        Producto categoria = productoRepository.findById(categoriaMenuId)
            .orElseThrow(() -> new NotFoundException("Categoría de menú no encontrada"));
        
        if (!TipoProducto.CATEGORIA_MENU.getCodigo().equals(categoria.getTipo().name())) {
            throw new BadRequestException("El producto no es una categoría de menú");
        }
    }

    private void validarProductoHijo(Long productoHijoId) {
        Producto producto = productoRepository.findById(productoHijoId)
            .orElseThrow(() -> new NotFoundException("Producto hijo no encontrado"));
        
        // Solo permitir MIXTO o COMPUESTO como hijos
        if (!TipoProducto.MIXTO.getCodigo().equals(producto.getTipo().name()) &&
            !TipoProducto.COMPUESTO.getCodigo().equals(producto.getTipo().name())) {
            throw new BadRequestException(
                "Solo se pueden agregar productos tipo MIXTO o COMPUESTO a una categoría de menú"
            );
        }
    }

    // ==================== CONVERSIÓN ====================

    private CategoriaMenuItemResponse convertirAResponse(ProductoCategoriaMenuItem item) {
        Producto productoHijo = productoRepository.findById(item.getProductoHijoId())
            .orElseThrow(() -> new NotFoundException("Producto hijo no encontrado"));
        
        CategoriaMenuItemResponse.ProductoHijoDTO productoHijoDTO = 
            CategoriaMenuItemResponse.ProductoHijoDTO.builder()
                .id(productoHijo.getId())
                .nombre(productoHijo.getNombre())
                .codigoInterno(productoHijo.getCodigoInterno())
                .tipo(productoHijo.getTipo().name())
                .precioVenta(productoHijo.getPrecioVenta())
                .imagenUrl(productoHijo.getImagenUrl())
                .descripcion(productoHijo.getDescripcion())
                .activo(productoHijo.getActivo())
                .build();
        
        return CategoriaMenuItemResponse.builder()
            .id(item.getId())
            .categoriaMenuId(item.getCategoriaMenuId())
            .productoHijo(productoHijoDTO)
            .orden(item.getOrden())
            .destacado(item.getDestacado())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
