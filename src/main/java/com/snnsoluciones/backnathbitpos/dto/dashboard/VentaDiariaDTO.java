package com.snnsoluciones.backnathbitpos.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para ventas de un día específico
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiariaDTO {
    
    /**
     * Fecha del día
     */
    private LocalDate fecha;
    
    /**
     * Monto total vendido ese día
     */
    private BigDecimal monto;
}