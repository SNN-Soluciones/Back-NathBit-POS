package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoUpdateDto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Handler encargado de todas las validaciones de productos.
 * Valida reglas de negocio, duplicados, y consistencia de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoValidador {

    private final ProductoRepository productoRepository;

    /**
     * Valida datos para crear un nuevo producto
     */
    public void validarCreacion(ProductoCreateDto dto) {
        log.debug("Validando creación de producto: {}", dto.getNombre());

        // Validaciones básicas
        validarDatosBasicos(dto.getNombre(), dto.getCodigoInterno(), dto.getPrecioVenta());

        // Validar duplicados
        Long empresaId = dto.getEmpresaId();
        
        if (existeCodigoInterno(dto.getCodigoInterno(), empresaId, null)) {
            throw new BusinessException(
                "Ya existe un producto con el código interno: " + dto.getCodigoInterno()
            );
        }

        if (dto.getCodigoBarras() != null && !dto.getCodigoBarras().isEmpty()) {
            if (existeCodigoBarras(dto.getCodigoBarras(), empresaId, null)) {
                throw new BusinessException(
                    "Ya existe un producto con el código de barras: " + dto.getCodigoBarras()
                );
            }
        }

        if (existeNombre(dto.getNombre(), empresaId, null)) {
            throw new BusinessException(
                "Ya existe un producto con el nombre: " + dto.getNombre()
            );
        }

        // Validaciones específicas por tipo
        if (dto.getTipo() != null) {
            validarSegunTipo(TipoProducto.valueOf(dto.getTipo()), dto);
        }

        log.debug("Validación de creación completada exitosamente");
    }

    /**
     * Valida datos para actualizar un producto existente
     */
    public void validarActualizacion(ProductoUpdateDto dto, Producto productoExistente) {
        log.debug("Validando actualización de producto ID: {}", productoExistente.getId());

        // Validaciones básicas
        if (dto.getNombre() != null) {
            validarDatosBasicos(dto.getNombre(), null, null);
        }

        Long empresaId = productoExistente.getEmpresa().getId();
        Long productoId = productoExistente.getId();

        // Validar duplicado de código interno (si cambió)
        if (dto.getCodigoInterno() != null && 
            !dto.getCodigoInterno().equals(productoExistente.getCodigoInterno())) {
            
            if (existeCodigoInterno(dto.getCodigoInterno(), empresaId, productoId)) {
                throw new BusinessException(
                    "Ya existe otro producto con el código interno: " + dto.getCodigoInterno()
                );
            }
        }

        // Validar duplicado de código de barras (si cambió)
        if (dto.getCodigoBarras() != null && 
            !dto.getCodigoBarras().equals(productoExistente.getCodigoBarras())) {
            
            if (existeCodigoBarras(dto.getCodigoBarras(), empresaId, productoId)) {
                throw new BusinessException(
                    "Ya existe otro producto con el código de barras: " + dto.getCodigoBarras()
                );
            }
        }

        // Validar duplicado de nombre (si cambió)
        if (dto.getNombre() != null && 
            !dto.getNombre().equals(productoExistente.getNombre())) {
            
            if (existeNombre(dto.getNombre(), empresaId, productoId)) {
                throw new BusinessException(
                    "Ya existe otro producto con el nombre: " + dto.getNombre()
                );
            }
        }

        // Validar precio (si cambió)
        if (dto.getPrecioVenta() != null) {
            validarDatosBasicos(null, null, dto.getPrecioVenta());
        }

        log.debug("Validación de actualización completada exitosamente");
    }

    /**
     * Valida datos básicos de un producto
     */
    private void validarDatosBasicos(String nombre, String codigoInterno, BigDecimal precioVenta) {
        
        // Validar nombre
        if (nombre != null) {
            if (nombre.isEmpty() || nombre.isBlank()) {
                throw new BusinessException("El nombre del producto no puede estar vacío");
            }
            if (nombre.length() > 200) {
                throw new BusinessException("El nombre del producto no puede exceder 200 caracteres");
            }
        }

        // Validar código interno
        if (codigoInterno != null) {
            if (codigoInterno.isEmpty() || codigoInterno.isBlank()) {
                throw new BusinessException("El código interno no puede estar vacío");
            }
            if (codigoInterno.length() > 20) {
                throw new BusinessException("El código interno no puede exceder 20 caracteres");
            }
        }

        // Validar precio
        if (precioVenta != null) {
            if (precioVenta.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("El precio de venta no puede ser negativo");
            }
            if (precioVenta.compareTo(new BigDecimal("999999999.99")) > 0) {
                throw new BusinessException("El precio de venta es demasiado alto");
            }
        }
    }

    /**
     * Validaciones específicas según el tipo de producto
     */
    private void validarSegunTipo(TipoProducto tipo, ProductoCreateDto dto) {
        switch (tipo) {
            case COMPUESTO:
                // Los productos compuestos se configuran después de crearse
                log.debug("Producto tipo COMPUESTO - se configurará después");
                break;
                
            case COMBO:
                // Los combos se configuran después de crearse
                log.debug("Producto tipo COMBO - se configurará después");
                break;
                
            case VENTA:
            case MIXTO:
                // Productos simples no requieren validaciones adicionales
                log.debug("Producto tipo {} - sin validaciones adicionales", tipo);
                break;
                
            default:
                log.warn("Tipo de producto no reconocido: {}", tipo);
        }
    }

    /**
     * Verifica si existe un producto con el código interno dado
     */
    public boolean existeCodigoInterno(String codigoInterno, Long empresaId, Long productoIdExcluir) {
        if (codigoInterno == null || codigoInterno.isEmpty()) {
            return false;
        }

        if (productoIdExcluir == null) {
            return productoRepository.existsByCodigoInternoAndEmpresaId(codigoInterno, empresaId);
        }

        // Excluir el producto actual en actualizaciones
        return productoRepository.findByCodigoInternoAndEmpresaId(codigoInterno, empresaId)
            .map(p -> !p.getId().equals(productoIdExcluir))
            .orElse(false);
    }

    /**
     * Verifica si existe un producto con el código de barras dado
     */
    public boolean existeCodigoBarras(String codigoBarras, Long empresaId, Long productoIdExcluir) {
        if (codigoBarras == null || codigoBarras.isEmpty()) {
            return false;
        }

        if (productoIdExcluir == null) {
            return productoRepository.existsByCodigoBarrasAndEmpresaId(codigoBarras, empresaId);
        }

        // Excluir el producto actual en actualizaciones
        return productoRepository.findByCodigoBarrasAndEmpresaId(codigoBarras, empresaId)
            .map(p -> !p.getId().equals(productoIdExcluir))
            .orElse(false);
    }

    /**
     * Verifica si existe un producto con el nombre dado
     */
    public boolean existeNombre(String nombre, Long empresaId, Long productoIdExcluir) {
        if (nombre == null || nombre.isEmpty()) {
            return false;
        }

        if (productoIdExcluir == null) {
            return productoRepository.existsByNombreAndEmpresaId(nombre, empresaId);
        }

        // Excluir el producto actual en actualizaciones
        return productoRepository.findByNombreAndEmpresaId(nombre, empresaId)
            .map(p -> !p.getId().equals(productoIdExcluir))
            .orElse(false);
    }

    /**
     * Valida que un producto puede ser eliminado
     */
    public void validarEliminacion(Producto producto) {
        log.debug("Validando eliminación de producto ID: {}", producto.getId());

        // Aquí puedes agregar validaciones adicionales
        // Por ejemplo: verificar si tiene facturas asociadas, inventario, etc.

        // Por ahora solo validamos que exista
        if (producto == null) {
            throw new BusinessException("El producto no existe");
        }

        log.debug("Validación de eliminación completada");
    }
}