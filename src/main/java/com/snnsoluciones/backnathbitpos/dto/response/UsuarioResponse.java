package com.snnsoluciones.backnathbitpos.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de respuesta con información del usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con información del usuario")
public class UsuarioResponse {

  @Schema(description = "ID único del usuario")
  private UUID id;

  @Schema(description = "Email del usuario", example = "usuario@ejemplo.com")
  private String email;

  @Schema(description = "Nombre del usuario", example = "Juan")
  private String nombre;

  @Schema(description = "Apellidos del usuario", example = "Pérez García")
  private String apellidos;

  @Schema(description = "Teléfono del usuario", example = "8888-8888")
  private String telefono;

  @Schema(description = "Número de identificación", example = "1-1234-5678")
  private String identificacion;

  @Schema(description = "Tipo de identificación", example = "FISICA")
  private String tipoIdentificacion;

  @Schema(description = "ID del rol asignado")
  private UUID rolId;

  @Schema(description = "Nombre del rol asignado", example = "CAJERO")
  private String rolNombre;

  @Schema(description = "ID de la sucursal predeterminada")
  private UUID sucursalPredeterminadaId;

  @Schema(description = "Nombre de la sucursal predeterminada", example = "Sucursal Principal")
  private String sucursalPredeterminadaNombre;

  @Schema(description = "IDs de las sucursales a las que tiene acceso")
  private Set<UUID> sucursalesIds;

  @Schema(description = "IDs de las cajas que puede operar")
  private Set<UUID> cajasIds;

  @Schema(description = "Fecha y hora del último acceso")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime ultimoAcceso;

  @Schema(description = "Número de intentos fallidos de login", example = "0")
  private Integer intentosFallidos;

  @Schema(description = "Indica si el usuario está bloqueado", example = "false")
  private Boolean bloqueado;

  @Schema(description = "Indica si el usuario está activo", example = "true")
  private Boolean activo;

  @Schema(description = "Fecha de creación del usuario")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @Schema(description = "Fecha de última actualización")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;

  @Schema(description = "ID del tenant al que pertenece", example = "demo")
  private String tenantId;
}