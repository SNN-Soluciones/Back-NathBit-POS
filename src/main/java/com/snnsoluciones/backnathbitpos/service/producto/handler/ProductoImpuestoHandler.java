package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.dto.producto.ImpuestoDto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.ProductoImpuestoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handler encargado de gestionar los impuestos de productos.
 * Maneja creación, actualización y eliminación de impuestos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoImpuestoHandler {

    private final ProductoImpuestoRepository impuestoRepository;

    /**
     * Crea impuestos para un producto
     */
    @Transactional
    public void crearImpuestos(Producto producto, List<ImpuestoDto> impuestosDto) {
        if (impuestosDto == null || impuestosDto.isEmpty()) {
            log.debug("No hay impuestos para crear");
            return;
        }

        log.debug("Creando {} impuestos para producto ID: {}", impuestosDto.size(), producto.getId());

        for (ImpuestoDto dto : impuestosDto) {
            validarImpuesto(dto);
            crearImpuesto(producto, dto);
        }

        log.info("Se crearon {} impuestos para producto ID: {}", impuestosDto.size(), producto.getId());
    }

    /**
     * Actualiza los impuestos de un producto (elimina los anteriores y crea nuevos)
     */
    @Transactional
    public void actualizarImpuestos(Producto producto, List<ImpuestoDto> impuestosDto) {
        log.debug("Actualizando impuestos para producto ID: {}", producto.getId());

        // Eliminar impuestos existentes
        eliminarImpuestos(producto);

        // Crear nuevos impuestos
        if (impuestosDto != null && !impuestosDto.isEmpty()) {
            crearImpuestos(producto, impuestosDto);
        }

        log.info("Impuestos actualizados para producto ID: {}", producto.getId());
    }

    /**
     * Elimina todos los impuestos de un producto
     */
    @Transactional
    public void eliminarImpuestos(Producto producto) {
        log.debug("Eliminando impuestos del producto ID: {}", producto.getId());

        List<ProductoImpuesto> impuestos = impuestoRepository.findByProductoId(producto.getId());

        if (!impuestos.isEmpty()) {
            impuestoRepository.deleteAll(impuestos);
            log.info("Se eliminaron {} impuestos del producto ID: {}", impuestos.size(), producto.getId());
        } else {
            log.debug("El producto no tenía impuestos asociados");
        }
    }

    /**
     * Crea un impuesto individual
     */
    private void crearImpuesto(Producto producto, ImpuestoDto dto) {
        ProductoImpuesto impuesto = ProductoImpuesto.builder()
            .producto(producto)
            .tipoImpuesto(TipoImpuesto.valueOf(dto.getTipoImpuesto()))
            .codigoTarifaIVA(CodigoTarifaIVA.valueOf(dto.getCodigoTarifa()))  // ← CORREGIDO: codigoTarifaIVA
            .porcentaje(dto.getTarifa())  // ← CORREGIDO: porcentaje en vez de tarifa
            .activo(true)
            .build();

        impuestoRepository.save(impuesto);
        log.debug("Impuesto creado: {} - Tarifa: {}%", dto.getTipoImpuesto(), dto.getTarifa());
    }

    /**
     * Valida los datos de un impuesto
     */
    private void validarImpuesto(ImpuestoDto dto) {
        if (dto.getTipoImpuesto() == null || dto.getTipoImpuesto().isEmpty()) {
            throw new BusinessException("El tipo de impuesto es requerido");
        }

        if (dto.getCodigoTarifa() == null || dto.getCodigoTarifa().isEmpty()) {
            throw new BusinessException("El código de tarifa es requerido");
        }

        if (dto.getTarifa() == null) {
            throw new BusinessException("La tarifa del impuesto es requerida");
        }

        if (dto.getTarifa().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La tarifa del impuesto no puede ser negativa");
        }

        if (dto.getTarifa().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("La tarifa del impuesto no puede exceder 100%");
        }

        // Validar que el tipo de impuesto sea válido
        try {
            TipoImpuesto.valueOf(dto.getTipoImpuesto());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo de impuesto inválido: " + dto.getTipoImpuesto());
        }

        // Validar que el código de tarifa sea válido
        try {
            CodigoTarifaIVA.valueOf(dto.getCodigoTarifa());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Código de tarifa inválido: " + dto.getCodigoTarifa());
        }
    }

    /**
     * Obtiene los impuestos de un producto como DTOs
     */
    public List<ImpuestoDto> obtenerImpuestosComoDto(Long productoId) {
        List<ProductoImpuesto> impuestos = impuestoRepository.findByProductoId(productoId);

        return impuestos.stream()
            .map(this::convertirADto)
            .toList();
    }

    /**
     * Convierte un ProductoImpuesto a DTO
     */
    private ImpuestoDto convertirADto(ProductoImpuesto impuesto) {
        return ImpuestoDto.builder()
            .tipoImpuesto(impuesto.getTipoImpuesto().name())
            .codigoTarifa(impuesto.getCodigoTarifaIVA().name())  // ← CORREGIDO
            .tarifa(impuesto.getPorcentaje())  // ← CORREGIDO: porcentaje en vez de tarifa
            .build();
    }
}