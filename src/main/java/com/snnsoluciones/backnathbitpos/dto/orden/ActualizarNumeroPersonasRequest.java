package com.snnsoluciones.backnathbitpos.dto.orden;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarNumeroPersonasRequest(
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 persona")
    Integer numeroPersonas
) {}