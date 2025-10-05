package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoSlotDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoOpcionDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoService;
import com.snnsoluciones.backnathbitpos.service.ProductoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final SucursalRepository sucursalRepository;
    private final ProductoInventarioService productoInventarioService;
    
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
                    for (ProductoCompuestoRequest.OpcionRequest opcionRequest : slotRequest.getOpciones()) {
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

    private void agregarOpcionASlot(ProductoCompuestoSlot slot, ProductoCompuestoRequest.OpcionRequest opcionRequest, Long empresaId, int orden) {
        // Validar y obtener el producto que será la opción
        Producto productoOpcion = productoRepository.findById(opcionRequest.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto opción no encontrado: " + opcionRequest.getProductoId()));

        // Validar que el producto pertenezca a la misma empresa
        if (!productoOpcion.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto opción no pertenece a la empresa");
        }

        // Validar tipo de producto - solo MIXTO, MATERIA_PRIMA o VENTA pueden ser opciones
        if (productoOpcion.getTipo() == TipoProducto.COMPUESTO || productoOpcion.getTipo() == TipoProducto.COMBO) {
            throw new BusinessException("Un producto compuesto o combo fijo no puede ser opción de otro compuesto");
        }

        // Crear la opción
        ProductoCompuestoOpcion opcion = new ProductoCompuestoOpcion();
        opcion.setSlot(slot);
        opcion.setProducto(productoOpcion); // ⚠️ AQUÍ ESTABA EL ERROR - No se asignaba el producto
        opcion.setPrecioAdicional(opcionRequest.getPrecioAdicional());
        opcion.setEsDefault(opcionRequest.getEsDefault() != null ? opcionRequest.getEsDefault() : false);
        opcion.setDisponible(opcionRequest.getDisponible() != null ? opcionRequest.getDisponible() : true);
        opcion.setOrden(opcionRequest.getOrden() != null ? opcionRequest.getOrden() : orden);

        opcionRepository.save(opcion);
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
                    opcionDto.setPrecioAdicional(opcion.getPrecioAdicional() != null ? opcion.getPrecioAdicional() : BigDecimal.ZERO);
                    return opcionDto;
                })
                .collect(Collectors.toList());
            
            slotDto.setOpciones(opcionDtos);
            slotDtos.add(slotDto);
        }
        
        dto.setSlots(slotDtos);
        return dto;
    }

    // En ProductoCompuestoServiceImpl.java, agregar estos métodos:

    @Override
    @Transactional(readOnly = true)
    public CalculoPrecioResponse calcularPrecio(Long productoId, Long sucursalId, List<Long> opcionesSeleccionadas) {
        log.info("Calculando precio para producto {} con {} opciones", productoId, opcionesSeleccionadas.size());

        // Obtener producto y validar
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (producto.getTipo() != TipoProducto.COMPUESTO) {
            throw new BusinessException("El producto no es de tipo COMPUESTO");
        }

        BigDecimal precioBase = producto.getPrecioBase() != null ? producto.getPrecioBase() : producto.getPrecioVenta();
        BigDecimal totalAdicionales = BigDecimal.ZERO;
        List<CalculoPrecioResponse.DetalleOpcion> detalles = new ArrayList<>();

        // Procesar cada opción seleccionada
        for (Long opcionId : opcionesSeleccionadas) {
            ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opción no encontrada: " + opcionId));

            // Verificar disponibilidad en sucursal
            boolean disponibleEnSucursal = verificarDisponibilidadEnSucursal(opcion, sucursalId);

            totalAdicionales = totalAdicionales.add(opcion.getPrecioAdicional());

            detalles.add(CalculoPrecioResponse.DetalleOpcion.builder()
                .opcionId(opcionId)
                .productoNombre(opcion.getProducto().getNombre())
                .slotNombre(opcion.getSlot().getNombre())
                .precioAdicional(opcion.getPrecioAdicional())
                .disponibleEnSucursal(disponibleEnSucursal)
                .build());
        }

        BigDecimal precioFinal = precioBase.add(totalAdicionales);

        return CalculoPrecioResponse.builder()
            .precioBase(precioBase)
            .totalAdicionales(totalAdicionales)
            .precioFinal(precioFinal)
            .detalleOpciones(detalles)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionSeleccionResponse validarSeleccion(Long productoId, Long sucursalId, List<Long> opcionesSeleccionadas) {
        log.info("Validando selección para producto {} en sucursal {}", productoId, sucursalId);

        ProductoCompuesto compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración de compuesto no encontrada"));

        List<ValidacionSeleccionResponse.ErrorValidacion> errores = new ArrayList<>();
        List<ValidacionSeleccionResponse.SlotValidacion> validacionesSlot = new ArrayList<>();
        boolean todasDisponibles = true;

        // Agrupar opciones seleccionadas por slot
        Map<Long, List<ProductoCompuestoOpcion>> opcionesPorSlot = new HashMap<>();
        for (Long opcionId : opcionesSeleccionadas) {
            ProductoCompuestoOpcion opcion = opcionRepository.findById(opcionId).orElse(null);
            if (opcion != null) {
                opcionesPorSlot.computeIfAbsent(opcion.getSlot().getId(), k -> new ArrayList<>()).add(opcion);
            }
        }

        // Validar cada slot
        List<ProductoCompuestoSlot> slots = slotRepository.findByCompuestoIdOrderByOrden(compuesto.getId());
        for (ProductoCompuestoSlot slot : slots) {
            List<ProductoCompuestoOpcion> opcionesEnSlot = opcionesPorSlot.getOrDefault(slot.getId(), new ArrayList<>());
            int cantidadSeleccionada = opcionesEnSlot.size();
            boolean cumpleRequisitos = true;

            // Validar cantidad mínima
            if (slot.getEsRequerido() && cantidadSeleccionada < slot.getCantidadMinima()) {
                errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
                    .campo("slot_" + slot.getId())
                    .mensaje(String.format("%s requiere mínimo %d opción(es)", slot.getNombre(), slot.getCantidadMinima()))
                    .tipoError("FALTA_REQUERIDO")
                    .build());
                cumpleRequisitos = false;
            }

            // Validar cantidad máxima
            if (cantidadSeleccionada > slot.getCantidadMaxima()) {
                errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
                    .campo("slot_" + slot.getId())
                    .mensaje(String.format("%s permite máximo %d opción(es)", slot.getNombre(), slot.getCantidadMaxima()))
                    .tipoError("EXCEDE_MAXIMO")
                    .build());
                cumpleRequisitos = false;
            }

            // Validar stock de cada opción
            List<ValidacionSeleccionResponse.OpcionValidada> opcionesValidadas = new ArrayList<>();
            for (ProductoCompuestoOpcion opcion : opcionesEnSlot) {
                boolean tieneStock = verificarStockOpcion(opcion, sucursalId);
                if (!tieneStock) {
                    todasDisponibles = false;
                    errores.add(ValidacionSeleccionResponse.ErrorValidacion.builder()
                        .campo("opcion_" + opcion.getId())
                        .mensaje(String.format("%s no tiene stock suficiente", opcion.getProducto().getNombre()))
                        .tipoError("SIN_STOCK")
                        .build());
                }

                opcionesValidadas.add(ValidacionSeleccionResponse.OpcionValidada.builder()
                    .opcionId(opcion.getId())
                    .productoNombre(opcion.getProducto().getNombre())
                    .tieneStockSuficiente(tieneStock)
                    .mensajeStock(tieneStock ? "Disponible" : "Sin stock")
                    .build());
            }

            validacionesSlot.add(ValidacionSeleccionResponse.SlotValidacion.builder()
                .slotId(slot.getId())
                .slotNombre(slot.getNombre())
                .esRequerido(slot.getEsRequerido())
                .cantidadMinima(slot.getCantidadMinima())
                .cantidadMaxima(slot.getCantidadMaxima())
                .cantidadSeleccionada(cantidadSeleccionada)
                .cumpleRequisitos(cumpleRequisitos)
                .opcionesSeleccionadas(opcionesValidadas)
                .build());
        }

        return ValidacionSeleccionResponse.builder()
            .esValida(errores.isEmpty())
            .todasDisponiblesEnSucursal(todasDisponibles)
            .errores(errores)
            .validacionPorSlot(validacionesSlot)
            .build();
    }

    // Agregar estos métodos privados en ProductoCompuestoServiceImpl:

    private boolean verificarDisponibilidadEnSucursal(ProductoCompuestoOpcion opcion, Long sucursalId) {
        // Primero verificar si la opción está marcada como disponible globalmente
        if (!opcion.getDisponible()) {
            return false;
        }

        Producto producto = opcion.getProducto();

        // Si el producto no maneja inventario, siempre está disponible
        if (producto.getTipoInventario() == TipoInventario.NINGUNO) {
            return true;
        }

        // Verificar si la sucursal maneja inventario
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        if (!sucursal.getManejaInventario()) {
            // Si la sucursal no maneja inventario, todo está disponible
            return true;
        }

        // Para productos MATERIA_PRIMA o MIXTO, verificar existencia en inventario
        if (producto.getTipo() == TipoProducto.MATERIA_PRIMA ||
            producto.getTipo() == TipoProducto.MIXTO ||
            producto.getTipo() == TipoProducto.VENTA) {

            try {
                ProductoInventario inventario = productoInventarioService
                    .obtenerInventario(producto.getId(), sucursalId);

                // Si no hay registro de inventario, no está disponible
                if (inventario == null) {
                    return false;
                }

                // Verificar que haya al menos algo de stock
                BigDecimal disponible = inventario.getCantidadActual()
                    .subtract(inventario.getCantidadBloqueada());

                return disponible.compareTo(BigDecimal.ZERO) > 0;

            } catch (Exception e) {
                log.warn("Error verificando disponibilidad para producto {} en sucursal {}: {}",
                    producto.getId(), sucursalId, e.getMessage());
                return false;
            }
        }

        return true;
    }

    private boolean verificarStockOpcion(ProductoCompuestoOpcion opcion, Long sucursalId) {
        Producto producto = opcion.getProducto();

        // Si no maneja inventario, siempre tiene "stock"
        if (producto.getTipoInventario() == TipoInventario.NINGUNO) {
            return true;
        }

        // Verificar configuración de sucursal
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        // Si la sucursal no maneja inventario o permite negativos
        if (!sucursal.getManejaInventario() || sucursal.getPermiteNegativos()) {
            return true;
        }

        try {
            // Para productos con inventario SIMPLE
            if (producto.getTipoInventario() == TipoInventario.SIMPLE) {
                BigDecimal stockDisponible = obtenerCantidadDisponible(producto.getId(), sucursalId);

                // Asumimos que necesitamos al menos 1 unidad
                // Aquí podrías ajustar según la cantidad típica usada en el compuesto
                return stockDisponible.compareTo(BigDecimal.ONE) >= 0;
            }

            // Para productos con RECETA, verificar si se puede producir
            if (producto.getTipoInventario() == TipoInventario.RECETA) {
                return inventarioService.puedeProducir(
                    producto.getEmpresa().getId(),
                    producto.getId(),
                    sucursalId,
                    BigDecimal.ONE
                );
            }

        } catch (Exception e) {
            log.warn("Error verificando stock para opción {}: {}", opcion.getId(), e.getMessage());
            return false;
        }

        return true;
    }

    // Método helper adicional que podrías necesitar en ProductoInventarioService
    public BigDecimal obtenerCantidadDisponible(Long productoId, Long sucursalId) {
        ProductoInventario inventario = productoInventarioService
            .obtenerInventario(productoId, sucursalId);

        if (inventario == null) {
            return BigDecimal.ZERO;
        }

        return inventario.getCantidadActual()
            .subtract(inventario.getCantidadBloqueada());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCompuestoDto> filtrarPorDisponibilidadSucursal(List<ProductoCompuestoDto> compuestos, Long sucursalId) {
        log.info("Filtrando {} compuestos por disponibilidad en sucursal {}", compuestos.size(), sucursalId);

        List<ProductoCompuestoDto> compuestosFiltrados = new ArrayList<>();

        for (ProductoCompuestoDto compuesto : compuestos) {
            ProductoCompuestoDto compuestoFiltrado = new ProductoCompuestoDto();
            // Copiar datos básicos
            BeanUtils.copyProperties(compuesto, compuestoFiltrado);
            compuestoFiltrado.setSlots(new ArrayList<>());

            boolean tieneOpcionesDisponibles = false;

            // Filtrar slots y opciones
            for (ProductoCompuestoSlotDto slot : compuesto.getSlots()) {
                ProductoCompuestoSlotDto slotFiltrado = new ProductoCompuestoSlotDto();
                BeanUtils.copyProperties(slot, slotFiltrado);
                slotFiltrado.setOpciones(new ArrayList<>());

                // Filtrar solo opciones disponibles en la sucursal
                for (ProductoCompuestoOpcionDto opcion : slot.getOpciones()) {
                    if (opcion.getDisponible()) {
                        ProductoCompuestoOpcion opcionEntity = opcionRepository.findById(opcion.getId())
                            .orElse(null);

                        if (opcionEntity != null && verificarDisponibilidadEnSucursal(opcionEntity, sucursalId)) {
                            slotFiltrado.getOpciones().add(opcion);
                            tieneOpcionesDisponibles = true;
                        }
                    }
                }

                // Solo agregar el slot si tiene opciones disponibles o es opcional
                if (!slotFiltrado.getOpciones().isEmpty() || !slot.getEsRequerido()) {
                    compuestoFiltrado.getSlots().add(slotFiltrado);
                }
            }

            // Solo incluir el compuesto si tiene opciones disponibles
            if (tieneOpcionesDisponibles) {
                compuestosFiltrados.add(compuestoFiltrado);
            }
        }

        log.info("Filtrados {} compuestos con disponibilidad", compuestosFiltrados.size());
        return compuestosFiltrados;
    }
}