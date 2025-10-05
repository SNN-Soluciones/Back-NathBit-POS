package com.snnsoluciones.backnathbitpos.dto.mr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RechazarFacturaRequest(
    @NotBlank(message = "La clave de Hacienda es requerida")
    String claveHacienda,
    
    @NotNull(message = "El ID de empresa es requerido")
    Long empresaId,

    @NotNull(message = "El ID de sucursal es requerido")
    Long sucursalId,

    @NotBlank(message = "El tipo de rechazo es requerido")
    @Pattern(regexp = "06|07", message = "Tipo de rechazo debe ser 06 (Parcial) o 07 (Total)")
    String tipoRechazo,
    
    @NotBlank(message = "La justificación es requerida")
    @Size(min = 5, max = 160, message = "La justificación debe tener entre 5 y 160 caracteres")
    String justificacion
) {}