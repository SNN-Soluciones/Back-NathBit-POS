// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2AbrirSesionRequest.java

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2AbrirSesionRequest {

    @NotNull(message = "Terminal requerida")
    private Long terminalId;

    @NotNull(message = "Monto inicial requerido")
    @DecimalMin(value = "0", message = "Monto inicial debe ser >= 0")
    private BigDecimal montoInicial;

    @NotBlank(message = "Modo gaveta requerido")
    @Pattern(regexp = "COMPARTIDA|INDIVIDUAL",
             message = "Modo gaveta debe ser COMPARTIDA o INDIVIDUAL")
    private String modoGaveta;

    private String observaciones;
}