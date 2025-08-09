package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalResumenDTO;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.auth.ContextoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de autenticación adaptado al nuevo modelo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final UsuarioRepository usuarioRepository;
  private final UsuarioEmpresaRepository usuarioEmpresaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final UsuarioMapper usuarioMapper;
  private final ContextoService contextoService;

  // Cache de tokens revocados (en producción usar Redis)
  private final Set<String> tokensRevocados = Collections.synchronizedSet(new HashSet<>());

  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    log.info("Intento de login para: {}", loginRequest.getEmail());

    try {
      // Autenticar
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              loginRequest.getEmail(),
              loginRequest.getPassword()
          )
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Obtener usuario
      Usuario usuario = usuarioRepository.findByEmailOrUsername(
          loginRequest.getEmail(), loginRequest.getEmail()
      ).orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

      // Validar estado del usuario
      validarEstadoUsuario(usuario);

      // Actualizar último acceso
      usuario.setUltimoAcceso(LocalDateTime.now());
      usuario.setIntentosFallidos(0);
      usuarioRepository.save(usuario);

      // Generar tokens
      // Para usuarios del sistema, generar token simple
      if (usuario.esRolSistema()) {
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // Construir respuesta según el rol
        LoginResponse response = construirLoginResponse(usuario, token, refreshToken);

        log.info("Login exitoso para usuario: {} con rol: {}", usuario.getEmail(), usuario.getRol());
        return response;
      }

      // Para otros usuarios, generar token con información básica
      // El contexto se establecerá después si es necesario
      String token = jwtTokenProvider.generateToken(
          usuario.getId(),
          usuario.getEmail(),
          null, // empresaId se establecerá al seleccionar contexto
          null, // sucursalId se establecerá al seleccionar contexto
          usuario.getRol()
      );
      String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getId(), usuario.getEmail());

      // Construir respuesta según el rol
      LoginResponse response = construirLoginResponse(usuario, token, refreshToken);

      log.info("Login exitoso para usuario: {} con rol: {}", usuario.getEmail(), usuario.getRol());
      return response;

    } catch (Exception e) {
      log.error("Error en login para: {}", loginRequest.getEmail(), e);

      // Incrementar intentos fallidos
      usuarioRepository.findByEmailOrUsername(loginRequest.getEmail(), loginRequest.getEmail())
          .ifPresent(u -> {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            u.setFechaUltimoIntento(LocalDateTime.now());

            // Bloquear después de 5 intentos
            if (u.getIntentosFallidos() >= 5) {
              u.setBloqueado(true);
              u.setFechaDesbloqueo(LocalDateTime.now().plusMinutes(30));
            }

            usuarioRepository.save(u);
          });

      throw new UnauthorizedException("Credenciales inválidas");
    }
  }

  @Override
  @Transactional
  public TokenResponse seleccionarContexto(SeleccionContextoRequest request) {
    Long usuarioId = obtenerUsuarioIdActual();

    log.info("Selección de contexto - Usuario: {}, Empresa: {}, Sucursal: {}",
        usuarioId, request.getEmpresaId(), request.getSucursalId());

    // Establecer contexto
    ContextoDTO contexto = contextoService.establecerContexto(
        usuarioId,
        request.getEmpresaId(),
        request.getSucursalId()
    );

    // Generar nuevo token con contexto
    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    String nuevoToken = jwtTokenProvider.generateToken(
        usuarioId,
        usuario.getEmail(),
        contexto.getEmpresaId(),
        contexto.getSucursalId(),
        usuario.getRol()
    );

    return TokenResponse.builder()
        .token(nuevoToken)
        .tipo("Bearer")
        .expiraEn(jwtTokenProvider.getExpirationTime())
        .contexto(contexto)
        .build();
  }

  @Override
  public TokenResponse refresh(RefreshTokenRequest refreshTokenRequest) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new UnauthorizedException("Refresh token inválido");
    }

    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
    Usuario usuario = usuarioRepository.findByEmailOrUsername(username, username)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

    // Generar nuevos tokens
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        usuario.getEmail(), null, Collections.emptyList()
    );

    String nuevoToken = jwtTokenProvider.generateToken(authentication);
    String nuevoRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    return TokenResponse.builder()
        .token(nuevoToken)
        .refreshToken(nuevoRefreshToken)
        .tipo("Bearer")
        .expiraEn(jwtTokenProvider.getExpirationTime())
        .build();
  }

  @Override
  public void logout(String token) {
    tokensRevocados.add(token);

    // Limpiar contexto del usuario
    Long usuarioId = obtenerUsuarioIdDesdeToken(token);
    contextoService.limpiarContexto(usuarioId);

    log.info("Logout exitoso para usuario: {}", usuarioId);
  }

  @Override
  public boolean validarToken(String token) {
    return jwtTokenProvider.validateToken(token) && !tokensRevocados.contains(token);
  }

  @Override
  public Long obtenerUsuarioIdDesdeToken(String token) {
    Long usuarioId = jwtTokenProvider.getUserIdFromToken(token);
    if (usuarioId != null) {
      return usuarioId;
    }

    // Fallback: obtener por username
    String username = jwtTokenProvider.getUsernameFromToken(token);
    return usuarioRepository.findByEmailOrUsername(username, username)
        .map(Usuario::getId)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
  }

  @Override
  public ContextoDTO obtenerContextoDesdeToken(String token) {
    // Primero intentar obtener del token directamente
    Long empresaId = jwtTokenProvider.getEmpresaIdFromToken(token);
    Long sucursalId = jwtTokenProvider.getSucursalIdFromToken(token);

    // Si hay contexto en el token, construirlo
    if (empresaId != null) {
      Long usuarioId = obtenerUsuarioIdDesdeToken(token);

      // Obtener información adicional de la base de datos
      UsuarioEmpresa ue = usuarioEmpresaRepository
          .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId)
          .orElse(null);

      if (ue != null) {
        return ContextoDTO.builder()
            .usuarioId(usuarioId)
            .empresaId(empresaId)
            .empresaNombre(ue.getEmpresa().getNombre())
            .empresaCodigo(ue.getEmpresa().getCodigo())
            .sucursalId(sucursalId)
            .sucursalNombre(sucursalId != null ? ue.getSucursal().getNombre() : null)
            .sucursalCodigo(sucursalId != null ? ue.getSucursal().getCodigo() : null)
            .permisos(ue.getPermisos())
            .build();
      }
    }

    // Si no hay contexto en el token, buscar en el servicio
    Long usuarioId = obtenerUsuarioIdDesdeToken(token);
    return contextoService.obtenerContextoActual(usuarioId);
  }

  /**
   * Construye la respuesta de login según el rol del usuario
   */
  private LoginResponse construirLoginResponse(Usuario usuario, String token, String refreshToken) {
    LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
        .token(token)
        .refreshToken(refreshToken)
        .usuario(usuarioMapper.toDto(usuario));

    // ROOT y SOPORTE - Acceso directo al dashboard del sistema
    if (usuario.esRolSistema()) {
      return builder
          .tipoAcceso("SISTEMA")
          .requiereSeleccion(false)
          .rutaDestino("/dashboard-sistema")
          .build();
    }

    // SUPER_ADMIN - Múltiples empresas
    if (usuario.getRol() == RolNombre.SUPER_ADMIN) {
      List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(usuario);

      return builder
          .tipoAcceso("EMPRESARIAL")
          .empresas(empresas)
          .requiereSeleccion(true)
          .rutaDestino("/dashboard-empresarial")
          .build();
    }

    // ADMIN - Una empresa con múltiples sucursales
    if (usuario.getRol() == RolNombre.ADMIN) {
      List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(usuario);

      // Los ADMIN normalmente tienen una sola empresa
      if (empresas.size() == 1) {
        LoginResponse.EmpresaAccesoDTO empresa = empresas.get(0);
        return builder
            .tipoAcceso("EMPRESARIAL")
            .empresas(empresas)
            .requiereSeleccion(empresa.getSucursales().size() > 1)
            .rutaDestino("/dashboard-sucursales/" + empresa.getId())
            .build();
      }

      return builder
          .tipoAcceso("EMPRESARIAL")
          .empresas(empresas)
          .requiereSeleccion(true)
          .rutaDestino("/dashboard-empresarial")
          .build();
    }

    // OPERATIVOS - Acceso directo o selección si tienen múltiples asignaciones
    if (usuario.esRolOperativo()) {
      Set<UsuarioEmpresa> asignaciones = usuario.getUsuarioEmpresas().stream()
          .filter(UsuarioEmpresa::esAsignacionVigente)
          .collect(Collectors.toSet());

      if (asignaciones.size() == 1) {
        // Acceso directo
        UsuarioEmpresa asignacion = asignaciones.iterator().next();
        LoginResponse.ContextoOperativoDTO contexto = construirContextoOperativo(asignacion);

        return builder
            .tipoAcceso("OPERATIVO")
            .contexto(contexto)
            .requiereSeleccion(false)
            .rutaDestino("/sistema")
            .build();
      } else {
        // Múltiples asignaciones - requiere selección
        List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(usuario);

        return builder
            .tipoAcceso("OPERATIVO")
            .empresas(empresas)
            .requiereSeleccion(true)
            .rutaDestino("/seleccionar-sucursal")
            .build();
      }
    }

    throw new BadRequestException("Rol no reconocido: " + usuario.getRol());
  }

  /**
   * Obtiene las empresas a las que tiene acceso el usuario
   */
  private List<LoginResponse.EmpresaAccesoDTO> obtenerEmpresasAcceso(Usuario usuario) {
    Map<Long, LoginResponse.EmpresaAccesoDTO> empresasMap = new LinkedHashMap<>();

    for (UsuarioEmpresa ue : usuario.getUsuarioEmpresas()) {
      if (!ue.esAsignacionVigente()) continue;

      Long empresaId = ue.getEmpresa().getId();

      LoginResponse.EmpresaAccesoDTO empresaDto = empresasMap.computeIfAbsent(
          empresaId,
          k -> LoginResponse.EmpresaAccesoDTO.builder()
              .id(ue.getEmpresa().getId())
              .codigo(ue.getEmpresa().getCodigo())
              .nombre(ue.getEmpresa().getNombre())
              .nombreComercial(ue.getEmpresa().getNombreComercial())
              .logo(ue.getEmpresa().getLogoUrl())
              .activa(ue.getEmpresa().getActiva())
              .sucursales(new ArrayList<>())
              .accesoTodasSucursales(false)
              .build()
      );

      if (ue.tieneAccesoTodasSucursales()) {
        empresaDto.setAccesoTodasSucursales(true);
        // Cargar todas las sucursales de la empresa
        ue.getEmpresa().getSucursales().stream()
            .filter(Sucursal::getActiva)
            .forEach(s -> empresaDto.getSucursales().add(
                LoginResponse.SucursalAccesoDTO.builder()
                    .id(s.getId())
                    .codigo(s.getCodigo())
                    .nombre(s.getNombre())
                    .direccion(s.getDireccion())
                    .activa(s.getActiva())
                    .esPrincipal(s.getEsPrincipal())
                    .build()
            ));
      } else if (ue.getSucursal() != null) {
        // Acceso a sucursal específica
        empresaDto.getSucursales().add(
            LoginResponse.SucursalAccesoDTO.builder()
                .id(ue.getSucursal().getId())
                .codigo(ue.getSucursal().getCodigo())
                .nombre(ue.getSucursal().getNombre())
                .direccion(ue.getSucursal().getDireccion())
                .activa(ue.getSucursal().getActiva())
                .esPrincipal(ue.getSucursal().getEsPrincipal())
                .build()
        );
      }
    }

    // Actualizar total de sucursales
    empresasMap.values().forEach(e ->
        e.setTotalSucursales(e.getSucursales().size())
    );

    return new ArrayList<>(empresasMap.values());
  }

  /**
   * Construye el contexto operativo para acceso directo
   */
  private LoginResponse.ContextoOperativoDTO construirContextoOperativo(UsuarioEmpresa asignacion) {
    return LoginResponse.ContextoOperativoDTO.builder()
        .empresa(EmpresaResumenDTO.builder()
            .id(asignacion.getEmpresa().getId())
            .nombre(asignacion.getEmpresa().getNombre())
            .build())
        .sucursal(asignacion.getSucursal() != null ?
            SucursalResumenDTO.builder()
                .id(asignacion.getSucursal().getId())
                .nombre(asignacion.getSucursal().getNombre())
                .build() : null)
        .permisos(construirPermisosDTO(asignacion.getPermisos()))
        .build();
  }

  /**
   * Convierte el mapa de permisos a PermisosDTO
   */
  private LoginResponse.PermisosDTO construirPermisosDTO(Map<String, Object> permisos) {
    // Implementación simplificada - expandir según necesidad
    return LoginResponse.PermisosDTO.builder()
        .accesoTotal(Boolean.TRUE.equals(permisos.get("acceso_total")))
        .build();
  }

  /**
   * Valida el estado del usuario
   */
  private void validarEstadoUsuario(Usuario usuario) {
    if (!usuario.getActivo()) {
      throw new UnauthorizedException("Usuario inactivo");
    }

    if (usuario.getBloqueado()) {
      if (usuario.getFechaDesbloqueo() != null &&
          usuario.getFechaDesbloqueo().isAfter(LocalDateTime.now())) {
        throw new UnauthorizedException("Usuario bloqueado hasta: " +
            usuario.getFechaDesbloqueo());
      } else {
        // Desbloquear si ya pasó el tiempo
        usuario.setBloqueado(false);
        usuario.setFechaDesbloqueo(null);
        usuario.setIntentosFallidos(0);
      }
    }

    if (usuario.getPasswordTemporal()) {
      log.warn("Usuario {} tiene contraseña temporal", usuario.getEmail());
    }
  }

  /**
   * Obtiene el ID del usuario actual desde el contexto de seguridad
   */
  private Long obtenerUsuarioIdActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    return usuarioRepository.findByEmailOrUsername(username, username)
        .map(Usuario::getId)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
  }
}