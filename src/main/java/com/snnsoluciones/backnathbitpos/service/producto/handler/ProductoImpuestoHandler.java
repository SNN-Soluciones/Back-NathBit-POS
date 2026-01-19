package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.dto.producto.CrearImpuestoDto;
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
 * Handler para gestionar impuestos de productos.
 * Se acopla a las entidades existentes: ProductoImpuesto con tipoImpuesto, codigoTarifaIVA, porcentaje
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoImpuestoHandler {

    private final ProductoImpuestoRepository impuestoRepository;

    /**
     * Crea impuestos para un producto desde CrearImpuestoDto
     */
    @Transactional
    public void crearImpuestos(Producto producto, List<CrearImpuestoDto> impuestosDto) {
        log.debug("Creando {} impuestos para producto ID: {}", impuestosDto.size(), producto.getId());

        if (impuestosDto == null || impuestosDto.isEmpty()) {
            return;
        }

        for (CrearImpuestoDto dto : impuestosDto) {
            validarImpuesto(dto);
            
            ProductoImpuesto impuesto = ProductoImpuesto.builder()
                .producto(producto)
                .tipoImpuesto(dto.getTipo())  // ← TipoImpuesto del DTO
                .porcentaje(dto.getTarifa())  // ← tarifa del DTO → porcentaje en entidad
                .activo(true)
                .build();

            // Si es IVA y viene código de tarifa, asignarlo
            if (dto.getTipo() == TipoImpuesto.IVA && dto.getCodigoTarifa() != null) {
                try {
                    CodigoTarifaIVA codigoTarifa = CodigoTarifaIVA.fromCodigo(dto.getCodigoTarifa());
                    impuesto.setCodigoTarifaIVA(codigoTarifa);
                    // Sobrescribir porcentaje con el del enum si existe
                    impuesto.setPorcentaje(codigoTarifa.getPorcentaje());
                } catch (Exception e) {
                    log.warn("Código tarifa IVA no encontrado: {}, usando tarifa manual", dto.getCodigoTarifa());
                }
            }

            impuestoRepository.save(impuesto);
        }

        log.debug("Impuestos creados exitosamente");
    }

    /**
     * Actualiza los impuestos de un producto (elimina los viejos y crea nuevos)
     */
    @Transactional
    public void actualizarImpuestos(Producto producto, List<CrearImpuestoDto> impuestosDto) {
        log.debug("Actualizando impuestos del producto ID: {}", producto.getId());

        // Eliminar impuestos existentes
        impuestoRepository.deleteByProductoId(producto.getId());

        // Crear nuevos impuestos
        if (impuestosDto != null && !impuestosDto.isEmpty()) {
            crearImpuestos(producto, impuestosDto);
        }
    }

    /**
     * Valida los datos de un impuesto
     */
    private void validarImpuesto(CrearImpuestoDto dto) {
        if (dto.getTipo() == null) {
            throw new BusinessException("El tipo de impuesto es requerido");
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
    }
}