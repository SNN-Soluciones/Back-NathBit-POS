package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor   // ⭐ AGREGAR ESTO
@AllArgsConstructor
public class ProductoCompuestoOpcionDto {
    private Long id;
    private Long productoId;
    private String nombre;         // ⭐ agregar si no existe
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal precioAdicional; // Puede ser positivo o negativo
    private Boolean esDefault;
    private Boolean disponible;
    private Integer orden;
}