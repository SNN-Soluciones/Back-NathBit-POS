package com.snnsoluciones.backnathbitpos.dto.productocompuesto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para una opción de pregunta inicial
 * Ejemplo: "Combo", "Sencillo"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionPreguntaInicialDTO {

    /**
     * ID de la opción
     */
    private Long id;

    /**
     * Nombre de la opción
     * Ejemplo: "Combo", "Sencillo"
     */
    private String nombre;

    /**
     * Descripción adicional (opcional)
     */
    private String descripcion;

    /**
     * Precio adicional de esta opción
     * Ejemplo: Combo = +₡50, Sencillo = ₡0
     */
    private BigDecimal precioAdicional;

    /**
     * ID de la configuración que se activa al elegir esta opción
     */
    private Long configuracionId;

    /**
     * Indica si esta es la opción por defecto
     */
    private Boolean esDefault;

    /**
     * Orden de presentación
     */
    private Integer orden;
}