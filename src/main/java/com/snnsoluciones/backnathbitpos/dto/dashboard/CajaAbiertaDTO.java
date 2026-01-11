package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para cajas abiertas en el dashboard
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaAbiertaDTO {
    
    /**
     * ID de la sesión de caja
     */
    private Long id;
    
    /**
     * Nombre de la sucursal
     */
    private String sucursalNombre;
    
    /**
     * Nombre completo del usuario que abrió la caja
     */
    private String usuario;
    
    /**
     * Monto inicial con el que se abrió la caja
     */
    private BigDecimal montoInicial;
    
    /**
     * Hora de apertura de la caja
     */
    private LocalDateTime horaApertura;
}