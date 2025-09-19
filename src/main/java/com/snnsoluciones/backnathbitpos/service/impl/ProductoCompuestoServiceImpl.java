package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoSlotDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoOpcionDto;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCompuestoServiceImpl implements ProductoCompuestoService {
    
    private final ProductoRepository productoRepository;
    private final ProductoCompuestoRepository compuestoRepository;
    private final ProductoCompuestoSlotRepository slotRepository;
    private final ProductoCompuestoOpcionRepository opcionRepository;
    private final ProductoInventarioService inventarioService;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public ProductoCompuestoDto crear(Long empresaId, Long productoId, ProductoCompuestoRequest request) {
        log.info("Creando configuración de compuesto para producto: {}", productoId);
        
        // Validar producto
        Producto producto = validarProducto(empresaId, productoId);
        
        // Validar que sea tipo COMPUESTO
        if (producto.getTipo() != TipoProducto.COMPUESTO) {
            throw new BusinessException("El producto debe ser tipo COMPUESTO");
        }
        
        // Validar que no exista configuración previa
        if (compuestoRepository.existsByProductoId(productoId)) {
            throw new BusinessException("El producto ya tiene configuración de compuesto");
        }
        
        // Crear compuesto
        ProductoCompuesto compuesto = new ProductoCompuesto();
        compuesto.setProducto(producto);
        compuesto.setInstruccionesPersonalizacion(request.getInstruccionesPersonalizacion());
        compuesto.setTiempoPreparacionExtra(request.getTiempoPreparacionExtra());
        
        compuesto = compuestoRepository.save(compuesto);
        
        // Crear slots y opciones
        if (request.getSlots() != null && !request.getSlots().isEmpty()) {
            int ordenSlot = 0;
            for (var slotRequest : request.getSlots()) {
                ProductoCompuestoSlot slot = new ProductoCompuestoSlot();
                slot.setCompuesto(compuesto);
                slot.setNombre(slotRequest.getNombre());
                slot.setDescripcion(slotRequest.getDescripcion());
                slot.setCantidadMinima(slotRequest.getCantidadMinima());
                slot.setCantidadMaxima(slotRequest.getCantidadMaxima());
                slot.setEsRequerido(slotRequest.getEsRequerido());
                slot.setOrden(slotRequest.getOrden() != null ? slotRequest.getOrden() : ordenSlot++);
                
                slot = slotRepository.save(slot);
                
                // Crear opciones del slot
                if (slotRequest.getOpciones() != null) {
                    int ordenOpcion = 0;
                    for (var opcionRequest : slotRequest.getOpciones()) {
                        agregarOpcionASlot(slot, opcionRequest, empresaId, ordenOpcion++);
                    }
                }
            }
        }
        
        return convertirADto(compuesto);
    }
    
    @Override
    @Transactional
    public ProductoCompuestoDto actualizar(Long empresaId, Long productoId, ProductoCompuestoRequest request) {
        log.info("Actualizando configuración de compuesto para producto: {}", productoId);
        
        validarProducto(empresaId, productoId);
        
        ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));
        
        // Actualizar datos básicos
        compuesto.setInstruccionesPersonalizacion(request.getInstruccionesPersonalizacion());
        compuesto.setTiempoPreparacionExtra(request.getTiempoPreparacionExtra());
        
        // Por simplicidad, eliminamos slots existentes y recreamos
        // (En producción sería mejor hacer un merge inteligente)
        slotRepository.deleteByCompuestoId(compuesto.getId());
        
        // Recrear slots
        if (request.getSlots() != null) {
            int ordenSlot = 0;
            for (var slotRequest : request.getSlots()) {
                ProductoCompuestoSlot slot = new ProductoCompuestoSlot();
                slot.setCompuesto(compuesto);
                slot.setNombre(slotRequest.getNombre());
                slot.setDescripcion(slotRequest.getDescripcion());
                slot.setCantidadMinima(slotRequest.getCantidadMinima());
                slot.setCantidadMaxima(slotRequest.getCantidadMaxima());
                slot.setEsRequerido(slotRequest.getEsRequerido());
                slot.setOrden(slotRequest.getOrden() != null ? slotRequest.getOrden() : ordenSlot++);
                
                slot = slotRepository.save(slot);
                
                // Crear opciones
                if (slotRequest.getOpciones() != null) {
                    int ordenOpcion = 0;
                    for (var opcionRequest : slotRequest.getOpciones()) {
                        agregarOpcionASlot(slot, opcionRequest, empresaId, ordenOpcion++);
                    }
                }
            }
        }
        
        compuesto = compuestoRepository.save(compuesto);
        return convertirADto(compuesto);
    }
    
    @Override
    @Transactional
    public void eliminar(Long empresaId, Long productoId) {
        log.info("Eliminando configuración de compuesto para producto: {}", productoId);
        
        validarProducto(empresaId, productoId);
        
        ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));
        
        compuestoRepository.delete(compuesto); // CASCADE eliminará slots y opciones
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductoCompuestoDto buscarPorProductoId(Long empresaId, Long productoId) {
        validarProducto(empresaId, productoId);
        
        ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));
        
        return convertirADto(compuesto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductoCompuestoDto> listarPorEmpresa(Long empresaId) {
        return productoRepository
            .findByEmpresaIdAndTipoAndActivoTrue(empresaId, TipoProducto.COMPUESTO)
            .stream()
            .map(producto -> {
                try {
                    return buscarPorProductoId(empresaId, producto.getId());
                } catch (ResourceNotFoundException e) {
                    log.warn("Producto compuesto sin configuración: {}", producto.getId());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean esCompuesto(Long productoId) {
        return compuestoRepository.existsByProductoId(productoId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validarSeleccion(Long productoId, Map<Long, List<Long>> seleccionPorSlot) {
        ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));
        
        List<ProductoCompuestoSlot> slots = slotRepository.findByCompuestoIdOrderByOrden(compuesto.getId());
        
        for (ProductoCompuestoSlot slot : slots) {
            List<Long> opcionesSeleccionadas = seleccionPorSlot.get(slot.getId());
            
            // Validar slot requerido
            if (slot.getEsRequerido() && (opcionesSeleccionadas == null || opcionesSeleccionadas.isEmpty())) {
                throw new BusinessException("El slot '" + slot.getNombre() + "' es requerido");
            }
            
            // Validar cantidad mínima y máxima
            if (opcionesSeleccionadas != null) {
                if (opcionesSeleccionadas.size() < slot.getCantidadMinima()) {
                    throw new BusinessException(
                        String.format("El slot '%s' requiere mínimo %d opciones", 
                            slot.getNombre(), slot.getCantidadMinima())
                    );
                }
                
                if (opcionesSeleccionadas.size() > slot.getCantidadMaxima()) {
                    throw new BusinessException(
                        String.format("El slot '%s' permite máximo %d opciones", 
                            slot.getNombre(), slot.getCantidadMaxima())
                    );
                }
                
                // Validar que las opciones pertenezcan al slot
                List<Long> opcionesDelSlot = opcionRepository.findBySlotId(slot.getId())
                    .stream()
                    .map(ProductoCompuestoOpcion::getId)
                    .collect(Collectors.toList());
                
                for (Long opcionId : opcionesSeleccionadas) {
                    if (!opcionesDelSlot.contains(opcionId)) {
                        throw new BusinessException("Opción inválida para el slot: " + slot.getNombre());
                    }
                }
            }
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularPrecioTotal(Long productoId, Map<Long, List<Long>> seleccionPorSlot) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        BigDecimal precioTotal = producto.getPrecioBase() != null ? producto.getPrecioBase() : BigDecimal.ZERO;
        
        // Sumar precios adicionales de las opciones seleccionadas
        for (Map.Entry<Long, List<Long>> entry : seleccionPorSlot.entrySet()) {
            for (Long opcionId : entry.getValue()) {
                ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));
                
                if (opcion.getPrecioAdicional() != null && opcion.getPrecioAdicional().compareTo(BigDecimal.ZERO) > 0) {
                    precioTotal = precioTotal.add(opcion.getPrecioAdicional());
                }
            }
        }
        
        return precioTotal;
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validarDisponibilidad(Long productoId, Long sucursalId, Map<Long, List<Long>> seleccionPorSlot) {
        for (List<Long> opcionIds : seleccionPorSlot.values()) {
            for (Long opcionId : opcionIds) {
                ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));
                
                if (opcion.getDisponible() != null && !opcion.getDisponible()) {
                    throw new BusinessException("Opción no disponible: " + opcion.getProducto().getNombre());
                }
                
                // Verificar inventario del producto de la opción
                try {
                    BigDecimal stock = inventarioService.obtenerInventario(opcion.getProducto().getId(), sucursalId)
                        .getCantidadActual();
                    
                    if (stock.compareTo(BigDecimal.ONE) < 0) {
                        throw new BusinessException("Sin stock: " + opcion.getProducto().getNombre());
                    }
                } catch (ResourceNotFoundException e) {
                    // Si no hay inventario, asumimos que no requiere control
                    log.debug("Producto sin control de inventario: {}", opcion.getProducto().getId());
                }
            }
        }
    }
    
    @Override
    @Transactional
    public void descontarInventario(Long productoId, Long sucursalId, Map<Long, List<Long>> seleccionPorSlot) {
        for (List<Long> opcionIds : seleccionPorSlot.values()) {
            for (Long opcionId : opcionIds) {
                ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));
                
                // Descontar inventario del producto asociado
                try {
                    inventarioService.reducirInventario(
                        opcion.getProducto().getId(),
                        sucursalId,
                        BigDecimal.ONE,
                        "Usado en compuesto personalizado"
                    );
                } catch (Exception e) {
                    log.warn("No se pudo descontar inventario para: {}", opcion.getProducto().getNombre());
                }
            }
        }
    }
    
    @Override
    @Transactional
    public void habilitarOpcion(Long slotId, Long opcionId) {
        ProductoCompuestoOpcion opcion = validarOpcion(slotId, opcionId);
        opcion.setDisponible(true);
        opcionRepository.save(opcion);
    }
    
    @Override
    @Transactional
    public void deshabilitarOpcion(Long slotId, Long opcionId) {
        ProductoCompuestoOpcion opcion = validarOpcion(slotId, opcionId);
        opcion.setDisponible(false);
        opcionRepository.save(opcion);
    }
    
    @Override
    @Transactional
    public void establecerOpcionPorDefecto(Long slotId, Long opcionId) {
        ProductoCompuestoOpcion opcion = validarOpcion(slotId, opcionId);
        
        // Quitar default de otras opciones del slot
        List<ProductoCompuestoOpcion> opcionesDelSlot = opcionRepository.findBySlotId(slotId);
        for (ProductoCompuestoOpcion op : opcionesDelSlot) {
            op.setEsDefault(false);
        }
        
        // Establecer nueva opción por defecto
        opcion.setEsDefault(true);
        opcionRepository.saveAll(opcionesDelSlot);
    }
    
    // ========== MÉTODOS PRIVADOS ==========
    
    private Producto validarProducto(Long empresaId, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa");
        }
        
        return producto;
    }
    
    private ProductoCompuestoOpcion validarOpcion(Long slotId, Long opcionId) {
        ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada"));
        
        if (!opcion.getSlot().getId().equals(slotId)) {
            throw new BusinessException("La opción no pertenece al slot especificado");
        }
        
        return opcion;
    }
    
    private void agregarOpcionASlot(ProductoCompuestoSlot slot, Object opcionRequest, Long empresaId, int orden) {
        // Este método necesitaría el DTO OpcionRequest que no está en la interfaz actual
        // Por ahora lo dejamos como placeholder
        // TODO: Implementar cuando se tenga el DTO OpcionRequest
    }
    
    private ProductoCompuestoDto convertirADto(ProductoCompuesto compuesto) {
        ProductoCompuestoDto dto = modelMapper.map(compuesto, ProductoCompuestoDto.class);
        dto.setProductoId(compuesto.getProducto().getId());
        
        // Cargar slots con sus opciones
        List<ProductoCompuestoSlot> slots = slotRepository.findByCompuestoIdOrderByOrden(compuesto.getId());
        List<ProductoCompuestoSlotDto> slotDtos = new ArrayList<>();
        
        for (ProductoCompuestoSlot slot : slots) {
            ProductoCompuestoSlotDto slotDto = modelMapper.map(slot, ProductoCompuestoSlotDto.class);
            
            // Cargar opciones del slot
            List<ProductoCompuestoOpcion> opciones = opcionRepository.findBySlotId(slot.getId());
            List<ProductoCompuestoOpcionDto> opcionDtos = opciones.stream()
                .map(opcion -> {
                    ProductoCompuestoOpcionDto opcionDto = modelMapper.map(opcion, ProductoCompuestoOpcionDto.class);
                    opcionDto.setProductoId(opcion.getProducto().getId());
                    opcionDto.setProductoNombre(opcion.getProducto().getNombre());
                    opcionDto.setProductoCodigo(opcion.getProducto().getCodigoInterno());
                    opcionDto.setPrecioAdicional(opcion.getProducto().getPrecioBase());
                    return opcionDto;
                })
                .collect(Collectors.toList());
            
            slotDto.setOpciones(opcionDtos);
            slotDtos.add(slotDto);
        }
        
        dto.setSlots(slotDtos);
        return dto;
    }
}