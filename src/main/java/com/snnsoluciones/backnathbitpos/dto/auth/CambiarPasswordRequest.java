package com.snnsoluciones.backnathbitpos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request para cambiar contraseña")
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    @Schema(description = "Contraseña actual", required = true)
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
        message = "La contraseña debe contener al menos: una mayúscula, una minúscula, un número y un carácter especial"
    )
    @Schema(description = "Nueva contraseña", required = true)
    private String passwordNueva;

    @NotBlank(message = "La confirmación es requerida")
    @Schema(description = "Confirmación de la nueva contraseña", required = true)
    private String passwordConfirmacion;
}
