package com.snnsoluciones.backnathbitpos.dto.productocompuesto;

import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO que representa el flujo inicial de configuración de un producto compuesto
 * Decide si mostrar pregunta inicial o ir directo a configuración default
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionFlujoDTO {

    /**
     * ID del producto compuesto
     */
    private Long productoId;

    /**
     * Nombre del producto
     */
    private String productoNombre;

    /**
     * Precio base del producto
     */
    private BigDecimal precioBase;

    /**
     * Indica si tiene pregunta inicial (Birriamen: Combo/Sencillo)
     * true = mostrar pregunta inicial
     * false = ir directo a configuración default
     */
    private Boolean tienePreguntaInicial;

    /**
     * Datos del slot de pregunta inicial (si existe)
     * Solo tiene valor si tienePreguntaInicial = true
     */
    private SlotPreguntaInicialDTO slotPreguntaInicial;

    /**
     * Configuración default (si NO tiene pregunta inicial)
     * Solo tiene valor si tienePreguntaInicial = false
     */
    private ProductoCompuestoConfiguracionDTO configuracionDefault;
}