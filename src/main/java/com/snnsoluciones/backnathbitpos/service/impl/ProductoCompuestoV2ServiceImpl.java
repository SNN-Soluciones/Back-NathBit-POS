package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.compuestoV2.OpcionV2Dto;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Dto;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Request;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.SlotV2Dto;
import com.snnsoluciones.backnathbitpos.entity.FamiliaProducto;
import com.snnsoluciones.backnathbitpos.entity.OpcionV2;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoV2;
import com.snnsoluciones.backnathbitpos.entity.SlotV2;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.FamiliaProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.OpcionV2Repository;
import com.snnsoluciones.backnathbitpos.repository.ProductoCompuestoV2Repository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.SlotV2Repository;
import com.snnsoluciones.backnathbitpos.service.ProductoCompuestoV2Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoCompuestoV2ServiceImpl implements ProductoCompuestoV2Service {

    private final ProductoRepository productoRepository;
    private final ProductoCompuestoV2Repository compuestoRepository;
    private final SlotV2Repository slotRepository;
    private final FamiliaProductoRepository familiaRepository;
    private final OpcionV2Repository opcionRepository;

    // ==================== CRUD ====================

    @Override
    @Transactional
    public ProductoCompuestoV2Dto crear(Long empresaId, Long productoId, ProductoCompuestoV2Request request) {
        log.info("Creando compuesto V2 para producto: {}", productoId);

        Producto producto = validarProducto(empresaId, productoId);

        if (producto.getTipo() != TipoProducto.COMPUESTO) {
            throw new BusinessException("El producto debe ser tipo COMPUESTO");
        }

        if (compuestoRepository.existsByProductoId(productoId)) {
            throw new BusinessException("El producto ya tiene configuración V2");
        }

        ProductoCompuestoV2 compuesto = ProductoCompuestoV2.builder()
            .producto(producto)
            .instruccionesPersonalizacion(request.getInstruccionesPersonalizacion())
            .slots(new ArrayList<>())
            .build();

        compuesto = compuestoRepository.save(compuesto);

        // Crear slots raíz
        if (request.getSlots() != null) {
            int orden = 0;
            for (var slotReq : request.getSlots()) {
                crearSlot(compuesto, null, slotReq, empresaId, orden++);
            }
        }

        log.info("Compuesto V2 creado exitosamente para producto: {}", productoId);
        return convertirADto(compuesto);
    }

    @Override
    @Transactional
    public ProductoCompuestoV2Dto actualizar(Long empresaId, Long productoId, ProductoCompuestoV2Request request) {
        log.info("Actualizando compuesto V2 para producto: {}", productoId);

        validarProducto(empresaId, productoId);

        ProductoCompuestoV2 compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Compuesto V2 no encontrado"));

        // Actualizar campos básicos
        compuesto.setInstruccionesPersonalizacion(request.getInstruccionesPersonalizacion());

        // Eliminar slots existentes y recrear (CASCADE elimina opciones y subslots)
        slotRepository.deleteByCompuestoId(compuesto.getId());
        slotRepository.flush();

        // Recrear slots
        if (request.getSlots() != null) {
            int orden = 0;
            for (var slotReq : request.getSlots()) {
                crearSlot(compuesto, null, slotReq, empresaId, orden++);
            }
        }

        compuesto = compuestoRepository.save(compuesto);
        log.info("Compuesto V2 actualizado exitosamente para producto: {}", productoId);
        return convertirADto(compuesto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoCompuestoV2Dto obtener(Long empresaId, Long productoId) {
        validarProducto(empresaId, productoId);

        ProductoCompuestoV2 compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Compuesto V2 no encontrado"));

        return convertirADto(compuesto);
    }

    @Override
    @Transactional
    public void eliminar(Long empresaId, Long productoId) {
        validarProducto(empresaId, productoId);

        ProductoCompuestoV2 compuesto = compuestoRepository.findByProductoId(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Compuesto V2 no encontrado"));

        compuestoRepository.delete(compuesto);
        log.info("Compuesto V2 eliminado para producto: {}", productoId);
    }

    // ==================== CREACIÓN RECURSIVA ====================

    private SlotV2 crearSlot(
        ProductoCompuestoV2 compuesto,
        OpcionV2 opcionPadre,
        ProductoCompuestoV2Request.SlotV2Request request,
        Long empresaId,
        int orden
    ) {
        // Validar familia si aplica
        FamiliaProducto familia = null;
        if (Boolean.TRUE.equals(request.getUsaFamilia())) {
            if (request.getFamiliaId() == null) {
                throw new BusinessException("Slot con familia requiere familiaId");
            }
            familia = familiaRepository.findById(request.getFamiliaId())
                .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));
        } else {
            if (request.getOpciones() == null || request.getOpciones().isEmpty()) {
                throw new BusinessException("Slot manual '" + request.getNombre() + "' requiere opciones");
            }
        }

        SlotV2 slot = SlotV2.builder()
            .compuesto(opcionPadre == null ? compuesto : null)  // solo raíz tiene compuesto
            .opcionPadre(opcionPadre)
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .esRequerido(request.getEsRequerido() != null ? request.getEsRequerido() : true)
            .cantidadMinima(request.getCantidadMinima() != null ? request.getCantidadMinima() : 1)
            .cantidadMaxima(request.getCantidadMaxima() != null ? request.getCantidadMaxima() : 1)
            .orden(request.getOrden() != null ? request.getOrden() : orden)
            .usaFamilia(request.getUsaFamilia() != null ? request.getUsaFamilia() : false)
            .familia(familia)
            .precioAdicionalPorOpcion(request.getPrecioAdicionalPorOpcion())
            .opciones(new ArrayList<>())
            .build();

        slot = slotRepository.save(slot);

        // Crear opciones (solo si es manual)
        if (!Boolean.TRUE.equals(slot.getUsaFamilia()) && request.getOpciones() != null) {
            int ordenOpcion = 0;
            for (var opcionReq : request.getOpciones()) {
                crearOpcion(slot, opcionReq, empresaId, ordenOpcion++);
            }
        }

        return slot;
    }

    private OpcionV2 crearOpcion(
        SlotV2 slot,
        ProductoCompuestoV2Request.OpcionV2Request request,
        Long empresaId,
        int orden
    ) {
        // Validar: debe tener productoId O nombre
        Producto producto = null;
        if (request.getProductoId() != null) {
            producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Producto no encontrado: " + request.getProductoId()
                ));
        } else {
            if (request.getNombre() == null || request.getNombre().isBlank()) {
                throw new BusinessException("Opción sin producto debe tener nombre");
            }
        }

        OpcionV2 opcion = OpcionV2.builder()
            .slot(slot)
            .nombre(request.getNombre())
            .producto(producto)
            .precioAdicional(request.getPrecioAdicional() != null ? request.getPrecioAdicional() : BigDecimal.ZERO)
            .esDefault(request.getEsDefault() != null ? request.getEsDefault() : false)
            .disponible(request.getDisponible() != null ? request.getDisponible() : true)
            .orden(request.getOrden() != null ? request.getOrden() : orden)
            .subSlots(new ArrayList<>())
            .build();

        opcion = opcionRepository.save(opcion);

        // ⭐ Crear sub-slots recursivamente
        if (request.getSubSlots() != null && !request.getSubSlots().isEmpty()) {
            int ordenSubSlot = 0;
            for (var subSlotReq : request.getSubSlots()) {
                crearSlot(null, opcion, subSlotReq, empresaId, ordenSubSlot++);
            }
        }

        return opcion;
    }

    // ==================== CONVERSIÓN A DTO (RECURSIVA) ====================

    private ProductoCompuestoV2Dto convertirADto(ProductoCompuestoV2 compuesto) {
        // Cargar slots raíz frescos desde BD
        List<SlotV2> slots = slotRepository.findByCompuestoIdOrderByOrden(compuesto.getId());

        return ProductoCompuestoV2Dto.builder()
            .id(compuesto.getId())
            .productoId(compuesto.getProducto().getId())
            .productoNombre(compuesto.getProducto().getNombre())
            .instruccionesPersonalizacion(compuesto.getInstruccionesPersonalizacion())
            .slots(slots.stream().map(this::convertirSlotADto).collect(Collectors.toList()))
            .createdAt(compuesto.getCreatedAt())
            .updatedAt(compuesto.getUpdatedAt())
            .build();
    }

    private SlotV2Dto convertirSlotADto(SlotV2 slot) {
        return SlotV2Dto.builder()
            .id(slot.getId())
            .nombre(slot.getNombre())
            .descripcion(slot.getDescripcion())
            .esRequerido(slot.getEsRequerido())
            .cantidadMinima(slot.getCantidadMinima())
            .cantidadMaxima(slot.getCantidadMaxima())
            .orden(slot.getOrden())
            .usaFamilia(slot.getUsaFamilia())
            .familiaId(slot.getFamilia() != null ? slot.getFamilia().getId() : null)
            .familiaNombre(slot.getFamilia() != null ? slot.getFamilia().getNombre() : null)
            .precioAdicionalPorOpcion(slot.getPrecioAdicionalPorOpcion())
            .opciones(slot.getOpciones().stream()
                .map(this::convertirOpcionADto)
                .collect(Collectors.toList()))
            .build();
    }

    private OpcionV2Dto convertirOpcionADto(OpcionV2 opcion) {
        return OpcionV2Dto.builder()
            .id(opcion.getId())
            .nombre(opcion.getNombreEfectivo())
            .productoId(opcion.getProducto() != null ? opcion.getProducto().getId() : null)
            .productoNombre(opcion.getProducto() != null ? opcion.getProducto().getNombre() : null)
            .precioAdicional(opcion.getPrecioAdicional())
            .esDefault(opcion.getEsDefault())
            .disponible(opcion.getDisponible())
            .orden(opcion.getOrden())
            // ⭐ Recursivo — sub-slots de esta opción
            .subSlots(opcion.getSubSlots().stream()
                .map(this::convertirSlotADto)
                .collect(Collectors.toList()))
            .build();
    }

    // ==================== HELPERS ====================

    private Producto validarProducto(Long empresaId, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("El producto no pertenece a la empresa");
        }

        return producto;
    }
}