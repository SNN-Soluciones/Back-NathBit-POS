package com.snnsoluciones.backnathbitpos.dto.confighacienda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadEconomicaRequest {

    @NotBlank(message = "El código de actividad es requerido")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String codigo;

    @NotBlank(message = "La descripción es requerida")
    @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
    private String descripcion;

    // Indica si es la actividad principal
    private Boolean esPrincipal = false;
}