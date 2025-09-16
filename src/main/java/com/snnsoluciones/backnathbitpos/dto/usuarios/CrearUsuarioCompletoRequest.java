package com.snnsoluciones.backnathbitpos.dto.usuarios;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearUsuarioCompletoRequest {

    // Datos básicos del usuario
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotNull(message = "El rol es obligatorio")
    private RolNombre rol;

    private String telefono;

    // Asignaciones - condicionales según el rol
    private Long empresaId;          // Para roles no-SUPER_ADMIN (una sola empresa)
    private List<Long> empresasIds;  // Para SUPER_ADMIN (múltiples empresas)
    private Long sucursalId;         // Para roles operativos (una sola sucursal)
    private List<Long> sucursalesIds; // Para ADMIN (múltiples sucursales)

    // Opcional - si no se envía, se genera una temporal
    private String password;
    private Boolean requiereCambioPassword;
    private String username;
}