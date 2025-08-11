package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// DTO para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImpuestoDto {
    private Long id;
    private TipoImpuestoDto tipoImpuesto;
    private TarifaIVADto tarifaIva;
    private BigDecimal porcentaje;
    private Boolean activo;
}
