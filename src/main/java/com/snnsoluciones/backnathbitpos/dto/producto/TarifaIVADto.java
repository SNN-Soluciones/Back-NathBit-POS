package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaIVADto {
    private Long id;
    private String codigoHacienda;
    private String descripcion;
    private BigDecimal porcentaje;
    private Boolean activo;
}
