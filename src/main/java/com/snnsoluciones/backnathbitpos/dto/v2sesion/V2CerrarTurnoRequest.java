// src/main/java/com/snnsoluciones/backnathbitpos/dto/v2sesion/V2CerrarTurnoRequest.java
// V2CerrarTurnoRequest.java — reemplazar completo

package com.snnsoluciones.backnathbitpos.dto.v2sesion;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2CerrarTurnoRequest {

    @NotNull(message = "Monto contado requerido")
    @DecimalMin(value = "0", message = "Monto contado debe ser >= 0")
    private BigDecimal montoContado;

    @NotNull(message = "Monto retirado requerido")
    @DecimalMin(value = "0", message = "Monto retirado debe ser >= 0")
    private BigDecimal montoRetirado;

    @NotNull(message = "Fondo de caja requerido")
    @DecimalMin(value = "0", message = "Fondo de caja debe ser >= 0")
    private BigDecimal fondoCaja;

    // Declarados por el cajero — opcionales
    private BigDecimal totalSinpeDeclarado;
    private BigDecimal totalTarjetaDeclarado;
    private BigDecimal totalTransferenciaDeclarado;

    private String observaciones;

    // Datafonos — opcionales
    private List<DatafonoDTO> datafonos;

    // Denominaciones — obligatorias (aunque sea vacías)
    @NotNull(message = "Denominaciones requeridas")
    private List<DenominacionDTO> denominaciones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatafonoDTO {
        @NotBlank  private String     datafono;
        @NotNull   private BigDecimal monto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DenominacionDTO {
        @NotBlank  private String  tipo;    // BILLETE | MONEDA
        @NotNull   private Integer valor;
        @NotNull   @Min(0) private Integer cantidad;
    }
}