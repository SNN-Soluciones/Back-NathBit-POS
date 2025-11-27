// src/main/java/com/snnsoluciones/backnathbitpos/dto/EstadoPagoDTO.java
package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPagoDTO {
    
    private boolean activo;
    private boolean suspendido;
    private String mensaje;
    private int diasRestantes; // Negativo si está vencido
    private LocalDate fechaVencimiento;
    private BigDecimal montoAdeudado;
    private TipoAlerta tipoAlerta;
    
    public enum TipoAlerta {
        SIN_ALERTA,        // Más de 3 días
        AVISO_3_DIAS,      // 3 días antes
        AVISO_2_DIAS,      // 2 días antes
        AVISO_1_DIA,       // 1 día antes
        VENCIDO_HOY,       // El mismo día
        VENCIDO_1_DIA,     // 1 día de mora
        VENCIDO_2_DIAS,    // 2 días de mora
        VENCIDO_3_DIAS,    // 3 días de mora (último aviso)
        SUSPENDIDO         // Más de 3 días de mora
    }
}