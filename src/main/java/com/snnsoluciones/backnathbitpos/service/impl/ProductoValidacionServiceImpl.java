package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.CategoriaProducto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CategoriaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.ProductoValidacionService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoValidacionServiceImpl implements ProductoValidacionService {
    
    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaRepository;
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigoInterno(String codigo, Long empresaId, Long excludeId) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }
        
        if (excludeId != null) {
            return productoRepository.existsByCodigoInternoAndEmpresaId(codigo, empresaId) &&
                   !productoRepository.findByCodigoInternoAndEmpresaId(codigo, empresaId)
                       .map(p -> p.getId().equals(excludeId))
                       .orElse(false);
        }
        
        return productoRepository.existsByCodigoInternoAndEmpresaId(codigo, empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigoBarras(String codigo, Long empresaId, Long excludeId) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }
        
        if (excludeId != null) {
            return productoRepository.existsByCodigoBarrasAndEmpresaId(codigo, empresaId) &&
                   !productoRepository.findByCodigoBarrasAndEmpresaId(codigo, empresaId)
                       .map(p -> p.getId().equals(excludeId))
                       .orElse(false);
        }
        
        return productoRepository.existsByCodigoBarrasAndEmpresaId(codigo, empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existeNombre(String nombre, Long empresaId, Long excludeId) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }
        
        if (excludeId != null) {
            return productoRepository.existsByNombreAndEmpresaId(nombre, empresaId) &&
                   !productoRepository.findByNombreAndEmpresaId(nombre, empresaId)
                       .map(p -> p.getId().equals(excludeId))
                       .orElse(false);
        }
        
        return productoRepository.existsByNombreAndEmpresaId(nombre, empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validarProductoParaVenta(Long productoId) {
        log.debug("Validando producto {} para venta", productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        if (!producto.getActivo()) {
            throw new BusinessException("El producto está inactivo y no puede venderse");
        }
        
        if (producto.getPrecioVenta() == null || producto.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El producto no tiene precio válido");
        }
        
        // Validar que tenga al menos un impuesto configurado para facturación electrónica
        if (producto.getEmpresa().getRequiereHacienda() && 
            (producto.getImpuestos() == null || producto.getImpuestos().isEmpty())) {
            throw new BusinessException("El producto requiere configuración de impuestos para facturación electrónica");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarCambioCategoria(Long productoId, Long categoriaId) {
        log.debug("Validando cambio de categoría para producto {} a categoría {}", productoId, categoriaId);

        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + productoId);
        }

        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("Categoría no encontrada: " + categoriaId);
        }

        // Validar que la categoría y el producto pertenezcan a la misma empresa
        Producto producto = productoRepository.findById(productoId).orElseThrow();
        CategoriaProducto categoria = categoriaRepository.findById(categoriaId).orElseThrow();

        if (!producto.getEmpresa().getId().equals(categoria.getEmpresa().getId())) {
            throw new BusinessException("La categoría no pertenece a la misma empresa del producto");
        }

        // AGREGAR VALIDACIÓN DE CONTEXTO
        boolean productoEsGlobal = (producto.getSucursal() == null);
        boolean categoriaEsGlobal = (categoria.getSucursal() == null);

        if (productoEsGlobal != categoriaEsGlobal) {
            throw new BusinessException(
                "No se puede asignar una categoría " +
                    (categoriaEsGlobal ? "global" : "local") +
                    " a un producto " +
                    (productoEsGlobal ? "global" : "local")
            );
        }

        // Si ambos son locales, deben ser de la misma sucursal
        if (!productoEsGlobal && !producto.getSucursal().getId().equals(categoria.getSucursal().getId())) {
            throw new BusinessException(
                "La categoría y el producto deben pertenecer a la misma sucursal"
            );
        }

        if (!categoria.getActivo()) {
            throw new BusinessException("La categoría está inactiva");
        }
    }
}