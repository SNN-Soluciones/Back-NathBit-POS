package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RecetaUpdateDTO extends RecetaDto {
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
}
