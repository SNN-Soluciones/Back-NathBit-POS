package com.snnsoluciones.backnathbitpos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO para crear un nuevo usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para crear un nuevo usuario")
public class UsuarioCreateRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    @Schema(description = "Email del usuario", example = "usuario@ejemplo.com")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "La contraseña debe contener mayúsculas, minúsculas y números")
    @Schema(description = "Contraseña del usuario")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del usuario", example = "Pérez García")
    private String apellidos;

    @Pattern(regexp = "^[0-9-]+$", message = "El teléfono solo puede contener números y guiones")
    @Size(max = 50, message = "El teléfono no puede exceder 50 caracteres")
    @Schema(description = "Teléfono del usuario", example = "8888-8888")
    private String telefono;

    @Size(max = 50, message = "La identificación no puede exceder 50 caracteres")
    @Schema(description = "Número de identificación", example = "1-1234-5678")
    private String identificacion;

    @Schema(description = "Tipo de identificación", example = "FISICA")
    private String tipoIdentificacion;

    @Schema(description = "ID del rol a asignar al usuario")
    private UUID rolId;

    @Schema(description = "ID de la sucursal predeterminada")
    private UUID sucursalPredeterminadaId;

    @Schema(description = "IDs de las sucursales a las que tendrá acceso el usuario")
    private Set<UUID> sucursalesIds;

    @Schema(description = "IDs de las cajas que podrá operar el usuario")
    private Set<UUID> cajasIds;
}