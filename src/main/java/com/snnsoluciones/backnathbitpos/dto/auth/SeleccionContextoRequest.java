package com.snnsoluciones.backnathbitpos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para seleccionar contexto de trabajo")
public class SeleccionContextoRequest {

    @NotNull(message = "La empresa es requerida")
    @Schema(description = "ID de la empresa", example = "1", required = true)
    private Long empresaId;

    @Schema(description = "ID de la sucursal (null = todas las sucursales)", example = "2")
    private Long sucursalId;
}