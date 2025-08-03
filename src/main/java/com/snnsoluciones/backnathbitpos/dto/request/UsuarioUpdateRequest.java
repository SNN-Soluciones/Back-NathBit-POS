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
 * DTO para actualizar un usuario existente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para actualizar un usuario")
public class UsuarioUpdateRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del usuario", example = "Pérez García")
    private String apellidos;

    @Email(message = "El email debe ser válido")
    @NotBlank(message = "El email es requerido")
    @Schema(description = "Email del usuario", example = "juan.perez@ejemplo.com")
    private String email;

    @Pattern(regexp = "^[0-9-]+$", message = "El teléfono solo puede contener números y guiones")
    @Size(max = 50, message = "El teléfono no puede exceder 50 caracteres")
    @Schema(description = "Teléfono del usuario", example = "8888-8888")
    private String telefono;

    @Size(max = 50, message = "La identificación no puede exceder 50 caracteres")
    @Schema(description = "Número de identificación", example = "1-1234-5678")
    private String identificacion;

    @Schema(description = "Tipo de identificación", example = "FISICA")
    private String tipoIdentificacion;

    @Schema(description = "ID del rol asignado al usuario")
    private UUID rolId;

    @Schema(description = "ID de la sucursal predeterminada")
    private UUID sucursalPredeterminadaId;

    @Schema(description = "IDs de las sucursales a las que tiene acceso el usuario")
    private Set<UUID> sucursalesIds;

    @Schema(description = "IDs de las cajas que puede operar el usuario")
    private Set<UUID> cajasIds;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private Boolean activo;

    @Schema(description = "Indica si el usuario está bloqueado", example = "false")
    private Boolean bloqueado;

    // No incluimos el password aquí, se maneja en un endpoint separado para cambio de contraseña
}