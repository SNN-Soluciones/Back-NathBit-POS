package com.snnsoluciones.backnathbitpos.dto.slots;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO unificado para opciones de slots
 * Soporta tanto opciones manuales como opciones de familia
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpcionSlotDTO {
    
    // IDs
    private Long opcionId;          // ID de la opción manual (si aplica)
    private Long productoId;        // ID del producto (siempre presente)
    
    // Datos del producto
    private String nombre;
    private String codigoInterno;
    private String imagen;          // URL de la imagen (opcional)
    
    // Precios
    private BigDecimal precioBase;           // Precio base del producto
    private BigDecimal precioAdicional;      // Precio adicional al seleccionar
    private Boolean esGratuita;              // Si es gratuita (precioAdicional = 0)
    
    // Estado y disponibilidad
    private Boolean disponible;              // Si está disponible (stock)
    private Boolean esDefault;               // Si es opción por defecto
    private Integer stockDisponible;         // Cantidad en stock
    
    // Metadata
    private Integer orden;                   // Orden de visualización
    private String origen;                   // "MANUAL" o "FAMILIA"
}