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
   * @param slot      Slot a validar
   * @param seleccion Selección del usuario
   * @throws BusinessException si la selección es inválida
   */
  public void validarSeleccion(ProductoCompuestoSlot slot, SlotSeleccionDTO seleccion) {
    // Calcular cantidad total
    int cantidadTotal = seleccion.getOpciones().stream()
        .mapToInt(SlotSeleccionDTO.OpcionSeleccionada::getCantidad)
        .sum();

    // Validar cantidad total (min/max)
    if (cantidadTotal < slot.getCantidadMinima()) {
      throw new BusinessException(
          slot.getNombre() + " requiere mínimo " + slot.getCantidadMinima()
      );
    }

    if (cantidadTotal > slot.getCantidadMaxima()) {
      throw new BusinessException(
          slot.getNombre() + " permite máximo " + slot.getCantidadMaxima()
      );
    }

    // ⭐ NUEVA VALIDACIÓN: máximo de opciones diferentes
    if (slot.getMaxOpcionesDiferentes() != null) {
      int opcionesDiferentes = seleccion.getOpciones().size();

      if (opcionesDiferentes > slot.getMaxOpcionesDiferentes()) {
        throw new BusinessException(
            slot.getNombre() + " permite máximo " +
                slot.getMaxOpcionesDiferentes() + " opciones diferentes"
        );
      }
    }

    // Validar permite_cantidad_por_opcion
    if (!slot.permiteCantidadPorOpcion()) {
      boolean todasSonUno = seleccion.getOpciones().stream()
          .allMatch(o -> o.getCantidad() == 1);

      if (!todasSonUno) {
        throw new BusinessException(
            slot.getNombre() + " no permite especificar cantidades"
        );
      }
    }
  }
}