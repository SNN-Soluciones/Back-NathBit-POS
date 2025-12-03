package com.snnsoluciones.backnathbitpos.dto.pagos;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoPagoDTO {

    private boolean activo;
    private boolean suspendido;
    private boolean mostrarAlerta;  // Para saber si el frontend debe mostrar algo
    private String mensaje;
    private LocalDate fechaVencimiento;
    private TipoAlerta tipoAlerta;

    // Campos informativos (sin montos)
    private int diasParaVencimiento;    // Positivo: faltan días, 0: hoy, Negativo: vencido
    private int diasGraciaRestantes;    // Solo aplica cuando está vencido

    public enum TipoAlerta {
        SIN_ALERTA,         // > 3 días - NO mostrar nada
        PROXIMO_VENCER,     // 1-3 días antes
        DIA_PAGO,           // El día exacto de pago
        EN_GRACIA,          // Vencido pero dentro del período de gracia
        ULTIMO_DIA_GRACIA,  // Último día antes de suspensión
        SUSPENDIDO          // Bloqueado
    }
}