package com.snnsoluciones.backnathbitpos.dto.response;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de respuesta para Usuario
 * Adaptado para el modelo multi-empresa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

  // Datos básicos del usuario global
  private UUID id;
  private String email;
  private String nombre;
  private String apellidos;
  private String telefono;
  private String identificacion;
  private TipoIdentificacion tipoIdentificacion;
  private Boolean activo;
  private Boolean bloqueado;
  private LocalDateTime ultimoAcceso;

  // Contexto de empresa/sucursal (depende del contexto actual)
  private UUID empresaId;
  private String empresaNombre;
  private UUID rolId; // Deprecado - usar rolNombre
  private RolNombre rolNombre;
  private Boolean esPropietario;

  // Sucursales en el contexto de la empresa actual
  private UUID sucursalPredeterminadaId;
  private String sucursalPredeterminadaNombre;
  private Set<UUID> sucursalesIds;

  // Información adicional del contexto actual
  private UUID sucursalActualId;
  private String sucursalActualNombre;
  private Boolean puedeVerMultiplesSucursales;

  // Cajas asignadas (solo en contexto de sucursal específica)
  private Set<UUID> cajasIds;

  // Permisos en la sucursal actual
  private Boolean puedeLeer;
  private Boolean puedeEscribir;
  private Boolean puedeEliminar;
  private Boolean puedeAprobar;

  // Empresas disponibles (para usuarios con acceso multi-empresa)
  private Integer cantidadEmpresas;
  private Boolean tieneAccesoMultiEmpresa;

  // Auditoría
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Método helper
  public String getNombreCompleto() {
    return nombre + " " + (apellidos != null ? apellidos : "");
  }

  public boolean esAdministrador() {
    return rolNombre == RolNombre.SUPER_ADMIN || rolNombre == RolNombre.ADMIN;
  }
}