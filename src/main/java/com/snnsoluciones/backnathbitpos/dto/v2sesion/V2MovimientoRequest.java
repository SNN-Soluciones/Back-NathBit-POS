// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2MovimientoRequest.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2MovimientoRequest {

    @NotBlank(message = "Tipo de movimiento requerido")
    private String tipo;
    // ENTRADA_EFECTIVO | ENTRADA_ABONO_CREDITO |
    // SALIDA_VALE | SALIDA_PAGO_PROVEEDOR | SALIDA_DEPOSITO | SALIDA_OTROS

    @NotNull(message = "Monto requerido")
    @DecimalMin(value = "0.01", message = "Monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotBlank(message = "Concepto requerido")
    private String concepto;

    private Long autorizadoPorId;
}