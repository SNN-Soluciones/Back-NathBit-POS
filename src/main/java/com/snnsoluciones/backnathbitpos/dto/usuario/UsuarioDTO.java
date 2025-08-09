package com.snnsoluciones.backnathbitpos.dto.usuario;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para transferencia de datos de Usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Información del usuario")
public class UsuarioDTO {

    @Schema(description = "ID del usuario", example = "1")
    private Long id;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Schema(description = "Email del usuario", example = "usuario@ejemplo.com", required = true)
    private String email;

    @Size(max = 50, message = "El username no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username solo puede contener letras, números, puntos, guiones y guiones bajos")
    @Schema(description = "Nombre de usuario único", example = "juan.perez")
    private String username;

    @Schema(description = "Contraseña del usuario", writeOnly = true)
    private String password;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    @Schema(description = "Nombre del usuario", example = "Juan", required = true)
    private String nombre;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos del usuario", example = "Pérez García")
    private String apellidos;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Teléfono inválido")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Schema(description = "Teléfono del usuario", example = "+506 8888-9999")
    private String telefono;

    @Size(max = 20, message = "La identificación no puede exceder 20 caracteres")
    @Schema(description = "Número de identificación", example = "1-1234-5678")
    private String identificacion;

    @Schema(description = "Rol global del usuario", example = "CAJERO", required = true)
    private RolNombre rol;

    @Schema(description = "Tipo de usuario", example = "EMPRESARIAL", required = true)
    private TipoUsuario tipoUsuario;

    @Schema(description = "Estado activo del usuario", example = "true")
    private Boolean activo;

    @Schema(description = "Usuario bloqueado", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean bloqueado;

    @Schema(description = "Número de intentos fallidos", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer intentosFallidos;

    @Schema(description = "Fecha del último intento de login", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaUltimoIntento;

    @Schema(description = "Fecha de desbloqueo", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaDesbloqueo;

    @Schema(description = "Último acceso exitoso", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime ultimoAcceso;

    @Schema(description = "Fecha del último cambio de contraseña", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime ultimoCambioPassword;

    @Schema(description = "Indica si tiene contraseña temporal", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean passwordTemporal;

    @Schema(description = "URL de la foto del usuario", example = "https://ejemplo.com/foto.jpg")
    private String fotoUrl;

    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    // Campos calculados (no mapeados directamente)

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García", accessMode = Schema.AccessMode.READ_ONLY)
    private String nombreCompleto;

    @Schema(description = "Indica si requiere selección de contexto", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean requiereSeleccionContexto;

    @Schema(description = "Indica si es usuario del sistema (ROOT/SOPORTE)", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean esRolSistema;

    @Schema(description = "Indica si es usuario administrativo", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean esRolAdministrativo;

    @Schema(description = "Indica si es usuario operativo", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean esRolOperativo;

    @Schema(description = "Número de empresas asignadas", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer empresasAsignadas;

    @Schema(description = "Número de sucursales asignadas", example = "2", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer sucursalesAsignadas;

    private String empresaActual;
    private String sucursalActual;
}