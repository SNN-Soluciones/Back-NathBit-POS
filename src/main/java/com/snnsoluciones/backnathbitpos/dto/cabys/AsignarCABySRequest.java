package com.snnsoluciones.backnathbitpos.dto.cabys;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarCABySRequest {
    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
    
    @NotNull(message = "El código CAByS es requerido")
    private Long codigoCabysId;
}