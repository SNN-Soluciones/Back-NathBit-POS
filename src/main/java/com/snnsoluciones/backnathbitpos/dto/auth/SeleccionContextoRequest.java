package com.snnsoluciones.backnathbitpos.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO para la selección de contexto (empresa/sucursal) después del login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeleccionContextoRequest {

    @NotNull(message = "La empresa es requerida")
    private UUID empresaId;

    @NotNull(message = "La sucursal es requerida")
    private UUID sucursalId;

    // Opcional: para mantener la sesión activa en este contexto
    private Boolean recordarSeleccion;
}
