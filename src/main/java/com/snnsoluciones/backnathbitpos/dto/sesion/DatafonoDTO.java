package com.snnsoluciones.backnathbitpos.dto.sesion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar el cierre de un datafono
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatafonoDTO {

    @NotBlank(message = "El nombre del datafono es requerido")
    private String datafono; // "BAC", "BCR", "Nacional", "Promerica", "Otro"

    @NotNull(message = "El monto es requerido")
    @Min(value = 0, message = "El monto debe ser mayor o igual a 0")
    private BigDecimal monto;
}