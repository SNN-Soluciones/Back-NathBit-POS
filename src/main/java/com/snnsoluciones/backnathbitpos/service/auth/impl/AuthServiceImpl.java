package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResumenDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalResumenDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
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
 * Implementación del servicio de autenticación adaptado al nuevo modelo con UsuarioEmpresaRol
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final UsuarioRepository usuarioRepository;
  private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
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
      Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
          .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

      // Validar estado del usuario
      validarEstadoUsuario(usuario);

      // Actualizar último acceso
      usuario.setFechaUltimoAcceso(LocalDateTime.now());
      usuario.setIntentosFallidos(0);
      usuarioRepository.save(usuario);

      // Obtener roles del usuario
      List<UsuarioEmpresaRol> rolesUsuario = usuarioEmpresaRolRepository.findByUsuarioId(usuario.getId());

      // Determinar el rol principal del usuario
      RolNombre rolPrincipal = determinarRolPrincipal(rolesUsuario);

      // Para usuarios ROOT y SOPORTE, generar token simple
      if (esRolSistema(rolPrincipal)) {
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        LoginResponse response = construirLoginResponse(usuario, rolesUsuario, rolPrincipal, token, refreshToken);
        log.info("Login exitoso para usuario: {} con rol: {}", usuario.getEmail(), rolPrincipal);
        return response;
      }

      // Para otros usuarios, generar token con información básica
      String token = jwtTokenProvider.generateToken(
          usuario.getId(),
          usuario.getEmail(),
          null, // empresaId se establecerá al seleccionar contexto
          null, // sucursalId se establecerá al seleccionar contexto
          rolPrincipal
      );
      String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getId(), usuario.getEmail());

      LoginResponse response = construirLoginResponse(usuario, rolesUsuario, rolPrincipal, token, refreshToken);
      log.info("Login exitoso para usuario: {} con rol: {}", usuario.getEmail(), rolPrincipal);
      return response;

    } catch (Exception e) {
      log.error("Error en login para: {}", loginRequest.getEmail(), e);

      // Incrementar intentos fallidos
      usuarioRepository.findByEmail(loginRequest.getEmail())
          .ifPresent(u -> {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            u.setFechaUltimoAcceso(LocalDateTime.now());

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

    // Validar que el usuario tiene acceso a ese contexto
    UsuarioEmpresaRol uer = usuarioEmpresaRolRepository
        .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, request.getEmpresaId(), request.getSucursalId())
        .orElseThrow(() -> new UnauthorizedException("No tiene acceso a este contexto"));

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
        uer.getRol()
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
    Usuario usuario = usuarioRepository.findByEmail(username)
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
    return usuarioRepository.findByEmail(username)
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
      UsuarioEmpresaRol uer = usuarioEmpresaRolRepository
          .findByUsuarioIdAndEmpresaIdAndSucursalId(usuarioId, empresaId, sucursalId)
          .orElse(null);

      if (uer != null) {
        return ContextoDTO.builder()
            .usuarioId(usuarioId)
            .empresaId(empresaId)
            .empresaNombre(uer.getEmpresa().getNombre())
            .empresaCodigo(uer.getEmpresa().getCodigo())
            .sucursalId(sucursalId)
            .sucursalNombre(sucursalId != null ? uer.getSucursal().getNombre() : null)
            .sucursalCodigo(sucursalId != null ? uer.getSucursal().getCodigo() : null)
            .rol(uer.getRol()) // Asumiendo que el ID es el ordinal + 1
            .build();
      }
    }

    // Si no hay contexto en el token, buscar en el servicio
    Long usuarioId = obtenerUsuarioIdDesdeToken(token);
    return contextoService.obtenerContextoActual(usuarioId);
  }

  /**
   * Determina el rol principal del usuario basado en la jerarquía
   */
  private RolNombre determinarRolPrincipal(List<UsuarioEmpresaRol> rolesUsuario) {
    if (rolesUsuario.isEmpty()) {
      throw new BadRequestException("Usuario sin roles asignados");
    }

    // Orden de prioridad: ROOT > SOPORTE > SUPER_ADMIN > ADMIN > OPERATIVOS
    return rolesUsuario.stream()
        .map(UsuarioEmpresaRol::getRol)
        .min((r1, r2) -> {
          List<RolNombre> jerarquia = Arrays.asList(
              RolNombre.ROOT,
              RolNombre.SOPORTE,
              RolNombre.SUPER_ADMIN,
              RolNombre.ADMIN,
              RolNombre.JEFE_CAJAS,
              RolNombre.CAJERO,
              RolNombre.MESERO,
              RolNombre.COCINA
          );
          return Integer.compare(jerarquia.indexOf(r1), jerarquia.indexOf(r2));
        })
        .orElseThrow(() -> new BadRequestException("No se pudo determinar el rol principal"));
  }

  /**
   * Verifica si es un rol del sistema (ROOT o SOPORTE)
   */
  private boolean esRolSistema(RolNombre rol) {
    return rol == RolNombre.ROOT || rol == RolNombre.SOPORTE;
  }

  /**
   * Verifica si es un rol operativo
   */
  private boolean esRolOperativo(RolNombre rol) {
    return rol == RolNombre.JEFE_CAJAS ||
        rol == RolNombre.CAJERO ||
        rol == RolNombre.MESERO ||
        rol == RolNombre.COCINA;
  }

  /**
   * Construye la respuesta de login según el rol del usuario
   */
  private LoginResponse construirLoginResponse(Usuario usuario, List<UsuarioEmpresaRol> rolesUsuario,
      RolNombre rolPrincipal, String token, String refreshToken) {
    LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
        .token(token)
        .refreshToken(refreshToken)
        .usuario(usuarioMapper.toDto(usuario));

    // ROOT y SOPORTE - Acceso directo al dashboard del sistema
    if (esRolSistema(rolPrincipal)) {
      return builder
          .tipoAcceso("SISTEMA")
          .requiereSeleccion(false)
          .rutaDestino("/dashboard-sistema")
          .build();
    }

    // SUPER_ADMIN - Múltiples empresas
    if (rolPrincipal == RolNombre.SUPER_ADMIN) {
      List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(rolesUsuario);

      return builder
          .tipoAcceso("EMPRESARIAL")
          .empresas(empresas)
          .requiereSeleccion(true)
          .rutaDestino("/dashboard-empresarial")
          .build();
    }

    // ADMIN - Una empresa con múltiples sucursales
    if (rolPrincipal == RolNombre.ADMIN) {
      List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(rolesUsuario);

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
    if (esRolOperativo(rolPrincipal)) {
      List<UsuarioEmpresaRol> asignacionesActivas = rolesUsuario.stream()
          .filter(uer -> uer.getActivo() &&
              uer.getEmpresa().getActiva() &&
              (uer.getSucursal() == null || uer.getSucursal().getActiva()))
          .collect(Collectors.toList());

      if (asignacionesActivas.size() == 1) {
        // Acceso directo
        UsuarioEmpresaRol asignacion = asignacionesActivas.get(0);
        LoginResponse.ContextoOperativoDTO contexto = construirContextoOperativo(asignacion);

        return builder
            .tipoAcceso("OPERATIVO")
            .contexto(contexto)
            .requiereSeleccion(false)
            .rutaDestino("/sistema")
            .build();
      } else {
        // Múltiples asignaciones - requiere selección
        List<LoginResponse.EmpresaAccesoDTO> empresas = obtenerEmpresasAcceso(rolesUsuario);

        return builder
            .tipoAcceso("OPERATIVO")
            .empresas(empresas)
            .requiereSeleccion(true)
            .rutaDestino("/seleccionar-sucursal")
            .build();
      }
    }

    throw new BadRequestException("Rol no reconocido: " + rolPrincipal);
  }

  /**
   * Obtiene las empresas a las que tiene acceso el usuario
   */
  private List<LoginResponse.EmpresaAccesoDTO> obtenerEmpresasAcceso(List<UsuarioEmpresaRol> rolesUsuario) {
    Map<Long, LoginResponse.EmpresaAccesoDTO> empresasMap = new LinkedHashMap<>();

    for (UsuarioEmpresaRol uer : rolesUsuario) {
      if (!uer.getActivo() || !uer.getEmpresa().getActiva()) continue;

      Long empresaId = uer.getEmpresa().getId();

      LoginResponse.EmpresaAccesoDTO empresaDto = empresasMap.computeIfAbsent(
          empresaId,
          k -> LoginResponse.EmpresaAccesoDTO.builder()
              .id(uer.getEmpresa().getId())
              .codigo(uer.getEmpresa().getCodigo())
              .nombre(uer.getEmpresa().getNombre())
              .nombreComercial(uer.getEmpresa().getNombreComercial())
              .logo(uer.getEmpresa().getLogoUrl())
              .activa(uer.getEmpresa().getActiva())
              .sucursales(new ArrayList<>())
              .accesoTodasSucursales(false)
              .build()
      );

      // Si tiene acceso sin sucursal específica, es acceso a todas
      if (uer.getSucursal() == null) {
        empresaDto.setAccesoTodasSucursales(true);
        // Cargar todas las sucursales de la empresa
        uer.getEmpresa().getSucursales().stream()
            .filter(Sucursal::getActiva)
            .forEach(s -> {
              // Evitar duplicados
              boolean yaExiste = empresaDto.getSucursales().stream()
                  .anyMatch(suc -> suc.getId().equals(s.getId()));

              if (!yaExiste) {
                empresaDto.getSucursales().add(
                    LoginResponse.SucursalAccesoDTO.builder()
                        .id(s.getId())
                        .codigo(s.getCodigo())
                        .nombre(s.getNombre())
                        .direccion(s.getDireccion())
                        .activa(s.getActiva())
                        .esPrincipal(s.getEsPrincipal())
                        .build()
                );
              }
            });
      } else if (uer.getSucursal().getActiva()) {
        // Acceso a sucursal específica
        boolean yaExiste = empresaDto.getSucursales().stream()
            .anyMatch(s -> s.getId().equals(uer.getSucursal().getId()));

        if (!yaExiste) {
          empresaDto.getSucursales().add(
              LoginResponse.SucursalAccesoDTO.builder()
                  .id(uer.getSucursal().getId())
                  .codigo(uer.getSucursal().getCodigo())
                  .nombre(uer.getSucursal().getNombre())
                  .direccion(uer.getSucursal().getDireccion())
                  .activa(uer.getSucursal().getActiva())
                  .esPrincipal(uer.getSucursal().getEsPrincipal())
                  .build()
          );
        }
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
  private LoginResponse.ContextoOperativoDTO construirContextoOperativo(UsuarioEmpresaRol asignacion) {
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
        .permisos(construirPermisosDTO(asignacion.getRol()))
        .build();
  }

  /**
   * Convierte los permisos del rol a PermisosDTO
   */
  private LoginResponse.PermisosDTO construirPermisosDTO(RolNombre rol) {
    // Implementación simplificada basada en el rol
    boolean accesoTotal = rol == RolNombre.ROOT ||
        rol == RolNombre.SUPER_ADMIN ||
        rol == RolNombre.ADMIN;

    return LoginResponse.PermisosDTO.builder()
        .accesoTotal(accesoTotal)
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

    return usuarioRepository.findByEmail(username)
        .map(Usuario::getId)
        .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
  }
}