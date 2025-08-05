package com.snnsoluciones.backnathbitpos.dto.request;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO para crear un nuevo usuario en el sistema multi-empresa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para crear un nuevo usuario")
public class UsuarioCreateRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Schema(description = "Email del usuario", example = "usuario@ejemplo.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Schema(description = "Contraseña del usuario", example = "Password123!")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del usuario", example = "Pérez García")
    private String apellidos;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Schema(description = "Teléfono del usuario", example = "+506 8888-8888")
    private String telefono;

    @Size(max = 50, message = "La identificación no puede exceder 50 caracteres")
    @Schema(description = "Número de identificación", example = "1-1234-5678")
    private String identificacion;

    @Schema(description = "Tipo de identificación")
    private TipoIdentificacion tipoIdentificacion;

    // Asignación inicial a empresa (obligatorio)
    @NotNull(message = "Debe especificar la empresa")
    @Schema(description = "ID de la empresa a la que se asignará el usuario")
    private UUID empresaId;

    @NotNull(message = "Debe especificar el rol")
    @Schema(description = "Rol del usuario en la empresa")
    private RolNombre rol;

    @Schema(description = "Indica si el usuario es propietario de la empresa", defaultValue = "false")
    @Builder.Default
    private Boolean esPropietario = false;

    // Asignación a sucursales (opcional, se puede hacer después)
    @Schema(description = "IDs de las sucursales a las que tendrá acceso el usuario")
    private Set<UUID> sucursalesIds;

    @Schema(description = "ID de la sucursal principal/predeterminada del usuario")
    private UUID sucursalPrincipalId;

    // Permisos por defecto (se pueden personalizar por sucursal después)
    @Schema(description = "Permiso de lectura en las sucursales", defaultValue = "true")
    @Builder.Default
    private Boolean puedeLeer = true;

    @Schema(description = "Permiso de escritura en las sucursales", defaultValue = "true")
    @Builder.Default
    private Boolean puedeEscribir = true;

    @Schema(description = "Permiso de eliminación en las sucursales", defaultValue = "false")
    @Builder.Default
    private Boolean puedeEliminar = false;

    @Schema(description = "Permiso de aprobación en las sucursales", defaultValue = "false")
    @Builder.Default
    private Boolean puedeAprobar = false;

    // Opciones adicionales
    @Schema(description = "Enviar email de bienvenida al usuario", defaultValue = "true")
    @Builder.Default
    private Boolean enviarEmailBienvenida = true;

    @Schema(description = "El usuario debe cambiar la contraseña en el primer inicio", defaultValue = "true")
    @Builder.Default
    private Boolean debeCambiarPassword = true;

    // Validación personalizada
    public boolean tienePermisosSuficientes() {
        // Al menos debe poder leer
        return Boolean.TRUE.equals(puedeLeer);
    }

    public boolean esUsuarioOperativo() {
        return rol == RolNombre.CAJERO || rol == RolNombre.MESERO;
    }

    public boolean requiereSucursalPrincipal() {
        // Usuarios operativos deben tener sucursal principal
        return esUsuarioOperativo() && sucursalPrincipalId == null;
    }
}