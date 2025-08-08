package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoAcceso;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import java.util.Comparator;
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
    log.debug("Intento de login para: {}", loginRequest.getEmail());

    try {
      // 1. Buscar usuario por email o username
      String loginIdentifier = loginRequest.getEmail();
      Usuario usuario = usuarioRepository.findByEmail(loginIdentifier)
          .orElseGet(() -> usuarioRepository.findByUsername(loginIdentifier)
              .orElseThrow(() -> new UnauthorizedException("Credenciales incorrectas")));

      // 2. Autenticar con Spring Security
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              usuario.getEmail(),
              loginRequest.getPassword()
          )
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      // 3. Validar que el usuario esté activo
      if (!usuario.getActivo()) {
        throw new DisabledException("Usuario inactivo");
      }

      // 4. Actualizar último acceso
      usuarioService.actualizarUltimoAcceso(usuario.getId());

      // 5. Preparar respuesta
      LoginResponse response = new LoginResponse();
      response.setUsuario(usuarioMapper.toDTO(usuario));

      // 6. Obtener accesos del usuario (roles en empresas)
      List<AccesoDTO> accesos = usuarioService.obtenerAccesos(usuario.getId());

      // 7. Determinar el rol global del usuario
      RolNombre rolGlobal = determinarRolGlobal(usuario, accesos);

      // 8. Lógica según el rol
      switch (Objects.requireNonNull(rolGlobal)) {
        case ROOT:
        case SOPORTE:
          // SISTEMA - Acceso total, ven todas las empresas
          log.info("Login exitoso para {}: {}", rolGlobal, usuario.getEmail());

          String tokenSistema = jwtTokenProvider.generateToken(
              usuario.getId(),
              usuario.getEmail(),
              null, // Sin empresa específica
              null, // Sin sucursal
              rolGlobal
          );

          response.setToken(tokenSistema);
          response.setTipoAcceso(TipoUsuario.SISTEMA);
          response.setRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getId()));
          response.setRequiereSeleccion(false);
          response.setMensaje("Acceso total al sistema");
          break;

        case SUPER_ADMIN:
          // SUPER_ADMIN - Puede o no tener empresas
          log.info("Login exitoso para SUPER_ADMIN: {} ({} empresas)",
              usuario.getEmail(), accesos.size());

          // Super Admin sin empresas aún
          String tokenSuperAdmin = jwtTokenProvider.generateToken(
              usuario.getId(),
              usuario.getEmail(),
              null,
              null,
              RolNombre.SUPER_ADMIN
          );

          response.setToken(tokenSuperAdmin);
          response.setTipoAcceso(TipoUsuario.EMPRESARIAL);
          response.setRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getId()));
          response.setRequiereSeleccion(false);
          response.setMensaje(
              "Login Exitoso.");
          break;

        case ADMIN:
          // ADMIN - Siempre tiene empresa asignada
          if (accesos.isEmpty()) {
            throw new UnauthorizedException("Usuario ADMIN sin empresa asignada");
          }

          // Admin normalmente tiene una sola empresa
          AccesoDTO acceso = accesos.get(0);
          String tokenAdmin = jwtTokenProvider.generateToken(
              usuario.getId(),
              usuario.getEmail(),
              acceso.getEmpresa().getId(),
              null, // Admin ve todas las sucursales
              RolNombre.ADMIN
          );

          response.setToken(tokenAdmin);
          response.setAccesoDirecto(acceso);
          response.setTipoAcceso(TipoUsuario.EMPRESARIAL);
          response.setRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getId()));
          response.setRequiereSeleccion(false);

          log.info("Login exitoso para ADMIN: {} en empresa {}",
              usuario.getEmail(), acceso.getEmpresa().getNombre());
          // Token temporal...
          break;

        case JEFE_CAJAS:
        case CAJERO:
        case MESERO:
        case COCINA:
          // OPERATIVOS - Siempre tienen sucursal específica
          if (accesos.isEmpty()) {
            throw new UnauthorizedException("Usuario operativo sin sucursal asignada");
          }

          // Validar que tenga sucursal asignada
          AccesoDTO accesoOperativo = accesos.stream()
              .filter(a -> a.getSucursal() != null)
              .findFirst()
              .orElseThrow(() -> new UnauthorizedException(
                  "Usuario operativo sin sucursal específica asignada"));

          // Login directo a su sucursal
          String tokenOperativo = jwtTokenProvider.generateToken(
              usuario.getId(),
              usuario.getEmail(),
              accesoOperativo.getEmpresa().getId(),
              accesoOperativo.getSucursal().getId(),
              accesoOperativo.getRol()
          );

          response.setToken(tokenOperativo);
          response.setAccesoDirecto(accesoOperativo);
          response.setTipoAcceso(TipoUsuario.OPERATIVO);
          response.setRequiereSeleccion(false);
          response.setRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getId()));

          log.info("Login exitoso para {} {}: {} en {}/{}",
              usuario.getTipoUsuario().getDisplayName(),
              accesoOperativo.getRol(),
              usuario.getEmail(),
              accesoOperativo.getEmpresa().getNombre(),
              accesoOperativo.getSucursal().getNombre());
          break;

        default:
          throw new UnauthorizedException("Tipo de usuario no reconocido");
      }

      return response;

    } catch (BadCredentialsException e) {
      log.warn("Credenciales incorrectas para: {}", loginRequest.getEmail());
      throw new UnauthorizedException("Credenciales incorrectas");
    } catch (DisabledException e) {
      log.warn("Usuario inactivo: {}", loginRequest.getEmail());
      throw e;
    } catch (Exception e) {
      log.error("Error en login: {}", e.getMessage(), e);
      throw new UnauthorizedException("Error en autenticación: " + e.getMessage());
    }
  }

  private RolNombre determinarRolGlobal(Usuario usuario, List<AccesoDTO> accesos) {
    if (usuario.getTipoUsuario().esSistema()) {
      if (usuario.getEmail().contains("root")) {
        return RolNombre.ROOT;
      } else {
        return RolNombre.SOPORTE; // Antes era DEVELOPER
      }
    }

    // Para otros usuarios, buscar el rol más alto
    if (accesos.isEmpty()) {
      // Sin accesos, pero podemos inferir por tipo de usuario
      if (usuario.getTipoUsuario() == TipoUsuario.EMPRESARIAL) {
        // Probablemente un SUPER_ADMIN recién creado
        return RolNombre.SUPER_ADMIN;
      }
      return null;
    }

    // Retornar el rol más alto de sus accesos
    return accesos.stream()
        .map(AccesoDTO::getRol)
        .min(Comparator.comparingInt(this::obtenerJerarquiaRol))
        .orElse(null);
  }

  private int obtenerJerarquiaRol(RolNombre rol) {
    return switch (rol) {
      case ROOT -> 1;
      case SOPORTE -> 2;
      case SUPER_ADMIN -> 3;
      case ADMIN -> 4;
      case JEFE_CAJAS -> 5;
      case CAJERO -> 6;
      case MESERO -> 7;
      case COCINA -> 8;
      default -> 99;
    };
  }

  // Método auxiliar para obtener el rol de usuarios SISTEMA
  private RolNombre obtenerRolSistema(Usuario usuario) {
    // Determinar rol basado en alguna lógica
    // Por ahora, asumimos que ROOT y DEVELOPER se identifican por email
    if (usuario.getEmail().contains("root")) {
      return RolNombre.ROOT;
    } else if (usuario.getEmail().contains("developer")) {
      return RolNombre.SOPORTE;
    }
    return RolNombre.SOPORTE; // Default para SISTEMA
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

  private TipoAcceso determinarTipoAcceso(List<AccesoDTO> accesos) {
    if (accesos.size() == 1) {
      AccesoDTO acceso = accesos.get(0);
      // Si solo tiene un acceso y es rol operativo
      if (esRolOperativo(acceso.getRol())) {
        return TipoAcceso.OPERATIVO;
      }
      return TipoAcceso.ADMINISTRATIVO;
    }
    return TipoAcceso.MULTIPLE;
  }

  private boolean esRolOperativo(RolNombre rol) {
    return rol == RolNombre.CAJERO ||
        rol == RolNombre.MESERO ||
        rol == RolNombre.COCINA;
  }

  private Long obtenerUsuarioIdActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Long) {
      return (Long) auth.getPrincipal();
    }
    throw new UnauthorizedException("Usuario no autenticado");
  }
}