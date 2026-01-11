package com.snnsoluciones.backnathbitpos.dto.dispositivo;

import lombok.*;

/**
 * DTO simple para listar sucursales en selección de registro PDV
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SucursalSimpleDTO {
    
    /**
     * ID de la sucursal
     */
    private Long id;
    
    /**
     * Nombre de la sucursal
     */
    private String nombre;
}