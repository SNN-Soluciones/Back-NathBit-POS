package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final UsuarioRepository usuarioRepository;

  @Value("${spring.security.jwt.expiration}")
  private Long jwtExpiration;

  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    log.info("Intento de login para: {}", loginRequest.getEmail());

    // Autenticar usuario
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        )
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Generar tokens
    String accessToken = tokenProvider.generateToken(authentication);
    String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getEmail());

    // Obtener usuario y actualizar último acceso
    Usuario usuario = (Usuario) authentication.getPrincipal();
    usuario.setUltimoAcceso(LocalDateTime.now());
    usuario.setIntentosFallidos(0);
    usuarioRepository.save(usuario);

    // Construir respuesta
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtExpiration)
        .email(usuario.getEmail())
        .nombre(usuario.getNombre())
        .apellidos(usuario.getApellidos())
        .roles(usuario.getRoles().stream()
            .map(rol -> rol.getNombre().name())
            .collect(Collectors.toList()))
        .tenantId(usuario.getTenantId())
        .build();
  }

  @Override
  public LoginResponse refreshToken(String refreshToken) {
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new RuntimeException("Refresh token inválido");
    }

    String username = tokenProvider.getUsernameFromToken(refreshToken);
    String newAccessToken = tokenProvider.generateTokenFromUsername(username);

    Usuario usuario = usuarioRepository.findByEmail(username)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    return LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtExpiration)
        .email(usuario.getEmail())
        .nombre(usuario.getNombre())
        .apellidos(usuario.getApellidos())
        .roles(usuario.getRoles().stream()
            .map(rol -> rol.getNombre().name())
            .collect(Collectors.toList()))
        .tenantId(usuario.getTenantId())
        .build();
  }

  @Override
  public void logout(String token) {
    // En una implementación real, podrías:
    // 1. Agregar el token a una blacklist
    // 2. Invalidar el token en Redis
    // 3. Registrar el evento de logout

    SecurityContextHolder.clearContext();
    log.info("Usuario deslogueado exitosamente");
  }
}