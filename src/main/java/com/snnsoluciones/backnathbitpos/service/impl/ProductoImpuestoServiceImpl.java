package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoImpuestoDto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoImpuestoRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.ProductoImpuestoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoImpuestoServiceImpl implements ProductoImpuestoService {
    
    private final ProductoImpuestoRepository impuestoRepository;
    private final ProductoRepository productoRepository;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductoImpuesto> obtenerImpuestos(Long productoId) {
        return impuestoRepository.findByProductoIdAndActivoTrue(productoId);
    }
    
    @Override
    @Transactional
    public ProductoImpuesto agregarImpuesto(Long productoId, ProductoImpuestoDto dto) {
        log.debug("Agregando impuesto {} al producto {}", dto.getTipoImpuesto(), productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        // Validar que no exista ya este tipo de impuesto
        if (impuestoRepository.existsByProductoIdAndTipoImpuesto(productoId, dto.getTipoImpuesto())) {
            throw new BusinessException("El producto ya tiene configurado el impuesto: " + dto.getTipoImpuesto());
        }
        
        ProductoImpuesto impuesto = new ProductoImpuesto();
        impuesto.setProducto(producto);
        impuesto.setTipoImpuesto(dto.getTipoImpuesto());
        impuesto.setActivo(true);
        
        // Configurar según el tipo
        if (dto.getTipoImpuesto() == TipoImpuesto.IVA) {
            if (dto.getCodigoTarifaIVA() == null) {
                throw new BusinessException("Debe especificar la tarifa IVA");
            }
            impuesto.setTarifaIva(dto.getCodigoTarifaIVA());
            impuesto.setPorcentaje(dto.getCodigoTarifaIVA().getPorcentaje());
        } else {
            if (dto.getPorcentaje() == null || dto.getPorcentaje().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Debe especificar un porcentaje válido");
            }
            impuesto.setPorcentaje(dto.getPorcentaje());
        }
        
        impuesto = impuestoRepository.save(impuesto);
        log.info("Impuesto {} agregado al producto {}", dto.getTipoImpuesto(), productoId);
        
        return impuesto;
    }
    
    @Override
    @Transactional
    public void actualizarImpuestos(Long productoId, List<ProductoImpuestoDto> impuestos) {
        log.debug("Actualizando impuestos del producto {}", productoId);
        
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        
        // Eliminar impuestos actuales
        impuestoRepository.deleteByProductoId(productoId);
        
        // Crear nuevos impuestos
        if (impuestos != null && !impuestos.isEmpty()) {
            for (ProductoImpuestoDto dto : impuestos) {
                ProductoImpuesto impuesto = new ProductoImpuesto();
                impuesto.setProducto(producto);
                impuesto.setTipoImpuesto(dto.getTipoImpuesto());
                impuesto.setActivo(true);
                
                if (dto.getTipoImpuesto() == TipoImpuesto.IVA) {
                    if (dto.getCodigoTarifaIVA() == null) {
                        throw new BusinessException("Debe especificar la tarifa IVA");
                    }
                    impuesto.setTarifaIva(dto.getCodigoTarifaIVA());
                    impuesto.setPorcentaje(dto.getCodigoTarifaIVA().getPorcentaje());
                } else {
                    if (dto.getPorcentaje() == null || dto.getPorcentaje().compareTo(BigDecimal.ZERO) < 0) {
                        throw new BusinessException("Debe especificar un porcentaje válido para " + dto.getTipoImpuesto());
                    }
                    impuesto.setPorcentaje(dto.getPorcentaje());
                }
                
                impuestoRepository.save(impuesto);
            }
        }
        
        log.info("Impuestos actualizados para producto {}", productoId);
    }
    
    @Override
    @Transactional
    public void quitarImpuesto(Long productoId, Long impuestoId) {
        log.debug("Quitando impuesto {} del producto {}", impuestoId, productoId);
        
        ProductoImpuesto impuesto = impuestoRepository.findById(impuestoId)
            .orElseThrow(() -> new ResourceNotFoundException("Impuesto no encontrado: " + impuestoId));
        
        if (!impuesto.getProducto().getId().equals(productoId)) {
            throw new BusinessException("El impuesto no pertenece al producto especificado");
        }
        
        impuestoRepository.deleteById(impuestoId);
        log.info("Impuesto {} eliminado del producto {}", impuestoId, productoId);
    }
    
    @Override
    @Transactional
    public void quitarTodosLosImpuestos(Long productoId) {
        log.debug("Quitando todos los impuestos del producto {}", productoId);
        
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + productoId);
        }
        
        impuestoRepository.deleteByProductoId(productoId);
        log.info("Todos los impuestos eliminados del producto {}", productoId);
    }
}