package com.snnsoluciones.backnathbitpos.dto.response;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoAcceso;
import com.snnsoluciones.backnathbitpos.dto.auth.SucursalDirecta;
import com.snnsoluciones.backnathbitpos.dto.auth.UsuarioInfo;
import com.snnsoluciones.backnathbitpos.enums.TipoFlujo;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * DTO para la respuesta del login en el nuevo sistema multi-empresa.
 * Soporta diferentes flujos según el tipo de usuario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

  // Tokens de autenticación
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Long expiresIn;

  // Tipo de flujo que debe seguir el frontend
  @NonNull
  private TipoFlujo tipoFlujo;

  // Lista de contextos disponibles (empresas/sucursales)
  private List<ContextoAcceso> contextosDisponibles;

  // Para usuarios operativos con acceso directo
  private SucursalDirecta sucursalDirecta;

  // Información del usuario
  private UsuarioInfo usuario;

  // Mensaje opcional
  private String mensaje;
}