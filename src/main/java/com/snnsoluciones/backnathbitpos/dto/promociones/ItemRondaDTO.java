package com.snnsoluciones.backnathbitpos.dto.promociones;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ítem inicial que se crea automáticamente al activar un AYCE o BARRA_LIBRE.
 * Se muestra al mesero antes de confirmar la aplicación.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRondaDTO {

    private Long productoId;
    private String nombreProducto;
    private Integer cantidad;
    private Integer maxRondas;       // null = ilimitado
}