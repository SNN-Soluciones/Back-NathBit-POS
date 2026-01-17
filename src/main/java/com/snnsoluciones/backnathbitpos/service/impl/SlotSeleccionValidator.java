package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.producto.SlotSeleccionDTO;
import com.snnsoluciones.backnathbitpos.entity.ProductoCompuestoSlot;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validador para selecciones de slots con cantidad por opción
 */
@Slf4j
@Component
public class SlotSeleccionValidator {

    /**
     * Valida que la selección de un slot cumple con las reglas
     * 
     * @param slot Slot a validar
     * @param seleccion Selección del usuario
     * @throws BusinessException si la selección es inválida
     */
    public void validarSeleccion(ProductoCompuestoSlot slot, SlotSeleccionDTO seleccion) {
        
        if (seleccion.getOpciones() == null || seleccion.getOpciones().isEmpty()) {
            if (Boolean.TRUE.equals(slot.getEsRequerido())) {
                throw new BusinessException(
                    "El slot '" + slot.getNombre() + "' es requerido"
                );
            }
            return; // Slot opcional sin selección está OK
        }

        // Calcular cantidad total seleccionada
        int cantidadTotal = seleccion.getOpciones().stream()
            .mapToInt(SlotSeleccionDTO.OpcionSeleccionada::getCantidad)
            .sum();

        // Validar cantidad mínima
        if (cantidadTotal < slot.getCantidadMinima()) {
            throw new BusinessException(
                String.format("El slot '%s' requiere mínimo %d %s. Seleccionaste: %d",
                    slot.getNombre(),
                    slot.getCantidadMinima(),
                    slot.getCantidadMinima() == 1 ? "opción" : "opciones",
                    cantidadTotal
                )
            );
        }

        // Validar cantidad máxima
        if (cantidadTotal > slot.getCantidadMaxima()) {
            throw new BusinessException(
                String.format("El slot '%s' permite máximo %d %s. Seleccionaste: %d",
                    slot.getNombre(),
                    slot.getCantidadMaxima(),
                    slot.getCantidadMaxima() == 1 ? "opción" : "opciones",
                    cantidadTotal
                )
            );
        }

        // Si el slot NO permite cantidad por opción, validar que todas sean 1
        if (!Boolean.TRUE.equals(slot.getPermiteCantidadPorOpcion())) {
            boolean tieneCantidadMayorA1 = seleccion.getOpciones().stream()
                .anyMatch(o -> o.getCantidad() != null && o.getCantidad() > 1);
            
            if (tieneCantidadMayorA1) {
                throw new BusinessException(
                    "El slot '" + slot.getNombre() + "' no permite especificar cantidad por opción"
                );
            }
        }

        log.debug("Selección válida para slot '{}': {} opciones, cantidad total: {}",
            slot.getNombre(), seleccion.getOpciones().size(), cantidadTotal);
    }
}