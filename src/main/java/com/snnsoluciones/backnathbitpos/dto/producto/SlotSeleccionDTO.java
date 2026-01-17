package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;

import java.util.List;

/**
 * DTO para recibir la selección de opciones de un slot
 * Soporta dos modos:
 * 1. Sin cantidad (comportamiento actual): solo IDs de opciones
 * 2. Con cantidad: ID + cantidad por cada opción
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotSeleccionDTO {

    /**
     * ID del slot
     */
    private Long slotId;

    /**
     * Opciones seleccionadas con sus cantidades
     * Si el slot NO permite cantidad por opción, cantidad siempre será 1
     */
    private List<OpcionSeleccionada> opciones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcionSeleccionada {
        
        /**
         * ID de la opción seleccionada
         */
        private Long opcionId;
        
        /**
         * Cantidad de esta opción
         * Si slot.permiteCantidadPorOpcion = false, este campo se ignora (siempre es 1)
         * Si slot.permiteCantidadPorOpcion = true, es requerido
         */
        @Builder.Default
        private Integer cantidad = 1;
    }
}