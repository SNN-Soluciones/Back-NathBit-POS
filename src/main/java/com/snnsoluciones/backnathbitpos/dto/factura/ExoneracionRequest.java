package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExoneracionRequest {

    @NotBlank(message = "Tipo de documento EX es requerido")
    private String tipoDocumentoEX; // catálogo MH

    @NotBlank(message = "Número de documento EX es requerido")
    private String numeroDocumentoEX;

    @NotNull(message = "Fecha de emisión EX es requerida")
    private LocalDate fechaEmisionExoneracion;

    @NotBlank(message = "Institución que otorga la exoneración es requerida")
    private String institucionOtorgante;

    @NotNull(message = "Porcentaje exonerado es requerido")
    @DecimalMin("0.00") @DecimalMax("100.00")
    private BigDecimal porcentajeExonerado;

    // Condicionales según tipoDocumentoEX
    private String articulo;
    private String inciso;
}