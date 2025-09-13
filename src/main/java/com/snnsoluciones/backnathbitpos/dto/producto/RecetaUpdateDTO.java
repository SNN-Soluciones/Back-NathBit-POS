package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RecetaUpdateDTO extends RecetaDTO {
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
}
