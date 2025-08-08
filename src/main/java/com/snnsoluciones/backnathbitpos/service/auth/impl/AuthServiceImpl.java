package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final UsuarioRepository usuarioRepository;
  private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
  private final UsuarioService usuarioService;
  private final UsuarioMapper usuarioMapper;

  // Cache simple para tokens revocados (en producción usar Redis)
  private final Set<String> tokensRevocados = new HashSet<>();

  @Override
  public LoginResponse login(LoginRequest loginRequest) {
    log.debug("Intento de login para email: {}", loginRequest.getEmail());

    // TODO Implementar logica segun el enum TipoUsuario
  }

  @Override
  public TokenResponse seleccionarContexto(SeleccionContextoRequest request) {
    Long usuarioId = obtenerUsuarioIdActual();

    // Validar que el usuario tenga acceso a la empresa/sucursal seleccionada
    if (!usuarioService.validarAcceso(usuarioId, request.getEmpresaId(), request.getSucursalId())) {
      throw new UnauthorizedException("No tiene acceso a la empresa/sucursal seleccionada");
    }

    // Obtener el rol específico para ese contexto
    UsuarioEmpresaRol usuarioRol;
    if (request.getSucursalId() != null) {
      usuarioRol = usuarioEmpresaRolRepository
          .findByUsuarioIdAndEmpresaIdAndSucursalId(
              usuarioId, request.getEmpresaId(), request.getSucursalId())
          .orElseThrow(() -> new UnauthorizedException("Rol no encontrado"));
    } else {
      usuarioRol = usuarioEmpresaRolRepository
          .findByUsuarioIdAndEmpresaIdAndSucursalIsNull(
              usuarioId, request.getEmpresaId())
          .orElseThrow(() -> new UnauthorizedException("Rol no encontrado"));
    }

    Usuario usuario = usuarioRol.getUsuario();

    // Generar token con contexto seleccionado
    String token = jwtTokenProvider.generateToken(
        usuario.getId(),
        usuario.getEmail(),
        request.getEmpresaId(),
        request.getSucursalId(),
        usuarioRol.getRol()
    );

    String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getId());

    TokenResponse response = new TokenResponse();
    response.setToken(token);
    response.setRefreshToken(refreshToken);
    response.setExpiresIn(jwtTokenProvider.getExpirationTime());

    log.info("Contexto seleccionado - Usuario: {}, Empresa: {}, Sucursal: {}",
        usuario.getEmail(), request.getEmpresaId(), request.getSucursalId());

    return response;
  }

  @Override
  public TokenResponse refresh(RefreshTokenRequest refreshTokenRequest) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new UnauthorizedException("Refresh token inválido");
    }

    Long usuarioId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    if (!usuario.getActivo()) {
      throw new UnauthorizedException("Usuario inactivo");
    }

    // Obtener contexto actual del token anterior si existe
    String authHeader = SecurityContextHolder.getContext()
        .getAuthentication().getCredentials().toString();

    Long empresaId = jwtTokenProvider.getEmpresaIdFromToken(authHeader);
    Long sucursalId = jwtTokenProvider.getSucursalIdFromToken(authHeader);
    RolNombre rol = jwtTokenProvider.getRolFromToken(authHeader);

    // Generar nuevo token con el mismo contexto
    String newToken = jwtTokenProvider.generateToken(
        usuario.getId(),
        usuario.getEmail(),
        empresaId,
        sucursalId,
        rol
    );

    String newRefreshToken = jwtTokenProvider.generateRefreshToken(usuario.getId());

    TokenResponse response = new TokenResponse();
    response.setToken(newToken);
    response.setRefreshToken(newRefreshToken);
    response.setExpiresIn(jwtTokenProvider.getExpirationTime());

    return response;
  }

  @Override
  public void logout(String token) {
    // Agregar token a lista de revocados
    tokensRevocados.add(token);

    // Limpiar contexto de seguridad
    SecurityContextHolder.clearContext();

    log.info("Logout exitoso para token: {}", token.substring(0, 20) + "...");
  }

  @Override
  public boolean validarToken(String token) {
    if (tokensRevocados.contains(token)) {
      return false;
    }
    return jwtTokenProvider.validateToken(token);
  }

  @Override
  public Long obtenerUsuarioIdDesdeToken(String token) {
    return jwtTokenProvider.getUserIdFromToken(token);
  }

  @Override
  public ContextoDTO obtenerContextoDesdeToken(String token) {
    ContextoDTO contexto = new ContextoDTO();
    contexto.setUsuarioId(jwtTokenProvider.getUserIdFromToken(token));
    contexto.setEmpresaId(jwtTokenProvider.getEmpresaIdFromToken(token));
    contexto.setSucursalId(jwtTokenProvider.getSucursalIdFromToken(token));
    contexto.setRol(jwtTokenProvider.getRolFromToken(token));
    return contexto;
  }

  // Métodos auxiliares privados


  private Long obtenerUsuarioIdActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Long) {
      return (Long) auth.getPrincipal();
    }
    throw new UnauthorizedException("Usuario no autenticado");
  }
}