package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoFlujo;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.global.ConfiguracionAccesoRepository;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación refactorizado para soportar multi-empresa.
 * Maneja diferentes flujos según el tipo de usuario.
 */
@Service("authServiceV2")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceV2 {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final ConfiguracionAccesoRepository configuracionAccesoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RateLimiterService rateLimiter;
    private final DeteccionSucursalService deteccionSucursalService;

    @Value("${app.jwt.expiration:3600}")
    private Long jwtExpiration;

    @Value("${app.security.max-intentos-login:3}")
    private int maxIntentosLogin;

    /**
     * Login principal que determina el flujo según el tipo de usuario
     */
    public LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String rateLimitKey = ipAddress + ":" + loginRequest.getEmail();

        // Verificar rate limiting
        if (rateLimiter.isBlocked(rateLimitKey)) {
            long minutesRemaining = rateLimiter.getBlockedMinutesRemaining(rateLimitKey);
            throw new BusinessException(String.format(
                "Demasiados intentos fallidos. Intente en %d minutos.", minutesRemaining
            ));
        }

        try {
            // 1. Buscar usuario global
            UsuarioGlobal usuario = usuarioGlobalRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

            // 2. Verificar contraseña
            if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPassword())) {
                registrarIntentoFallido(usuario, ipAddress);
                rateLimiter.loginFailed(rateLimitKey, loginRequest);
                throw new BadCredentialsException("Credenciales incorrectas");
            }

            // 3. Verificaciones de estado
            verificarEstadoUsuario(usuario);

            // 4. Login exitoso - actualizar usuario
            rateLimiter.loginSucceeded(rateLimitKey);
            usuario.setIntentosFallidos(0);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioGlobalRepository.save(usuario);

            // 5. Determinar el flujo según el tipo de usuario
            return determinarFlujoLogin(usuario, ipAddress, userAgent);

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error en login para usuario: {}", loginRequest.getEmail(), e);
            throw new BusinessException("Error al procesar el login");
        }
    }

    /**
     * Determina el flujo de login según las características del usuario
     */
    private LoginResponse determinarFlujoLogin(UsuarioGlobal usuario, String ipAddress, String userAgent) {
        List<UsuarioEmpresa> accesosActivos = usuario.getUsuarioEmpresas().stream()
            .filter(UsuarioEmpresa::getActivo)
            .filter(ue -> ue.getEmpresa().getActiva())
            .collect(Collectors.toList());

        // Sin accesos activos
        if (accesosActivos.isEmpty()) {
            return construirRespuestaSinAcceso(usuario);
        }

        // Usuario operativo (cajero/mesero)
        if (usuario.esUsuarioOperativo()) {
            return procesarLoginOperativo(usuario, accesosActivos, ipAddress);
        }

        // Usuario administrativo con múltiples empresas
        if (accesosActivos.size() > 1 || tieneMultiplesSucursales(accesosActivos)) {
            return construirRespuestaSelector(usuario, accesosActivos);
        }

        // Usuario administrativo con una sola empresa/sucursal
        return procesarLoginDirectoAdmin(usuario, accesosActivos.get(0));
    }

    /**
     * Procesa el login de un usuario operativo (cajero/mesero)
     */
    private LoginResponse procesarLoginOperativo(UsuarioGlobal usuario,
                                                  List<UsuarioEmpresa> accesos, 
                                                  String ipAddress) {
        // Intentar detectar la sucursal automáticamente
        EmpresaSucursal sucursalDetectada = deteccionSucursalService.detectarPorIP(ipAddress);
        
        if (sucursalDetectada == null) {
            // Buscar sucursal principal del usuario
            sucursalDetectada = buscarSucursalPrincipal(accesos);
        }

        if (sucursalDetectada == null) {
            // Si no hay detección ni principal, mostrar selector
            return construirRespuestaSelector(usuario, accesos);
        }

        // Verificar que el usuario tiene acceso a esa sucursal
        UsuarioSucursal accesoSucursal = verificarAccesoSucursal(usuario, sucursalDetectada);
        if (accesoSucursal == null) {
            return construirRespuestaSelector(usuario, accesos);
        }

        // Generar token con contexto completo
        String accessToken = tokenProvider.generateTokenWithFullContext(
            usuario.getEmail(),
            sucursalDetectada.getEmpresa().getId(),
            sucursalDetectada.getId(),
            sucursalDetectada.getSchemaName(),
            accesoSucursal.getUsuarioEmpresa().getRol().name()
        );

        String refreshToken = tokenProvider.generateRefreshToken(usuario.getEmail());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpiration)
            .tipoFlujo(TipoFlujo.DIRECTO_POS)
            .sucursalDirecta(SucursalDirecta.builder()
                .empresaId(sucursalDetectada.getEmpresa().getId())
                .empresaNombre(sucursalDetectada.getEmpresa().getNombreMostrar())
                .sucursalId(sucursalDetectada.getId())
                .sucursalNombre(sucursalDetectada.getNombreSucursal())
                .schemaName(sucursalDetectada.getSchemaName())
                .rol(accesoSucursal.getUsuarioEmpresa().getRol().name())
                .urlRedirect("/pos")
                .build())
            .usuario(construirUsuarioInfo(usuario))
            .mensaje("Acceso directo a " + sucursalDetectada.getNombreSucursal())
            .build();
    }

    /**
     * Construye respuesta para usuarios que necesitan seleccionar contexto
     */
    private LoginResponse construirRespuestaSelector(UsuarioGlobal usuario,
                                                      List<UsuarioEmpresa> accesos) {
        // Token temporal sin contexto específico
        String accessToken = tokenProvider.generateTemporaryToken(usuario.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(usuario.getEmail());

        List<ContextoAcceso> contextos = accesos.stream()
            .map(ue -> {
                List<SucursalInfo> sucursales = ue.getSucursalesConAcceso().stream()
                    .filter(EmpresaSucursal::getActiva)
                    .map(s -> SucursalInfo.builder()
                        .sucursalId(s.getId())
                        .codigoSucursal(s.getCodigoSucursal())
                        .nombreSucursal(s.getNombreSucursal())
                        .schemaName(s.getSchemaName())
                        .esPrincipal(s.getEsPrincipal())
                        .activa(s.getActiva())
                        .build())
                    .collect(Collectors.toList());

                return ContextoAcceso.builder()
                    .empresaId(ue.getEmpresa().getId())
                    .empresaCodigo(ue.getEmpresa().getCodigo())
                    .empresaNombre(ue.getEmpresa().getNombreMostrar())
                    .empresaLogo(ue.getEmpresa().getLogoUrl())
                    .rol(ue.getRol().name())
                    .esPropietario(ue.getEsPropietario())
                    .sucursalesDisponibles(sucursales)
                    .cantidadSucursales(sucursales.size())
                    .build();
            })
            .collect(Collectors.toList());

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(300L) // Token temporal de 5 minutos
            .tipoFlujo(TipoFlujo.SELECTOR_EMPRESA)
            .contextosDisponibles(contextos)
            .usuario(construirUsuarioInfo(usuario))
            .mensaje("Seleccione la empresa y sucursal a la que desea acceder")
            .build();
    }

    /**
     * Selección de contexto después del login inicial
     */
    public SeleccionContextoResponse seleccionarContexto(SeleccionContextoRequest request, 
                                                         String userEmail) {
        // Validar que el usuario tiene acceso
        UsuarioGlobal usuario = usuarioGlobalRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        // Buscar acceso a la empresa
        UsuarioEmpresa accesoEmpresa = usuario.getAccesoEmpresa(request.getEmpresaId());
        if (accesoEmpresa == null || !accesoEmpresa.getActivo()) {
            throw new UnauthorizedException("No tiene acceso a esta empresa");
        }

        // Buscar sucursal
        EmpresaSucursal sucursal = accesoEmpresa.getEmpresa().getSucursales().stream()
            .filter(s -> s.getId().equals(request.getSucursalId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("Sucursal no encontrada"));

        // Verificar acceso a la sucursal
        UsuarioSucursal accesoSucursal = accesoEmpresa.getUsuarioSucursales().stream()
            .filter(us -> us.getSucursal().getId().equals(request.getSucursalId()))
            .filter(UsuarioSucursal::getActivo)
            .findFirst()
            .orElseThrow(() -> new UnauthorizedException("No tiene acceso a esta sucursal"));

        // Generar nuevo token con contexto completo
        String accessToken = tokenProvider.generateTokenWithFullContext(
            usuario.getEmail(),
            accesoEmpresa.getEmpresa().getId(),
            sucursal.getId(),
            sucursal.getSchemaName(),
            accesoEmpresa.getRol().name()
        );

        String refreshToken = tokenProvider.generateRefreshToken(usuario.getEmail());

        // Determinar URL de redirección según rol
        String urlRedirect = determinarUrlRedirect(accesoEmpresa.getRol());

        return SeleccionContextoResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtExpiration)
            .contexto(ContextoSeleccionado.builder()
                .empresaId(accesoEmpresa.getEmpresa().getId())
                .empresaNombre(accesoEmpresa.getEmpresa().getNombreMostrar())
                .sucursalId(sucursal.getId())
                .sucursalNombre(sucursal.getNombreSucursal())
                .schemaName(sucursal.getSchemaName())
                .rol(accesoEmpresa.getRol().name())
                .esPropietario(accesoEmpresa.getEsPropietario())
                .build())
            .urlRedirect(urlRedirect)
            .build();
    }

    // Métodos auxiliares
    private void verificarEstadoUsuario(UsuarioGlobal usuario) {
        if (!usuario.getActivo()) {
            throw new BusinessException("Usuario inactivo");
        }
        if (usuario.getBloqueado()) {
            throw new BusinessException("Usuario bloqueado");
        }
        if (usuario.getDebeCambiarPassword()) {
            // Podríamos retornar un flag especial en lugar de bloquear
            log.info("Usuario {} debe cambiar su contraseña", usuario.getEmail());
        }
    }

    private void registrarIntentoFallido(UsuarioGlobal usuario, String ipAddress) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= maxIntentosLogin) {
            usuario.setBloqueado(true);
            log.warn("Usuario {} bloqueado por exceder intentos de login desde IP {}", 
                    usuario.getEmail(), ipAddress);
        }
        usuarioGlobalRepository.save(usuario);
    }

    private boolean tieneMultiplesSucursales(List<UsuarioEmpresa> accesos) {
        return accesos.stream()
            .mapToLong(ue -> ue.getSucursalesConAcceso().size())
            .sum() > 1;
    }

    private EmpresaSucursal buscarSucursalPrincipal(List<UsuarioEmpresa> accesos) {
        return accesos.stream()
            .flatMap(ue -> ue.getUsuarioSucursales().stream())
            .filter(UsuarioSucursal::getActivo)
            .filter(UsuarioSucursal::getEsPrincipal)
            .map(UsuarioSucursal::getSucursal)
            .findFirst()
            .orElse(null);
    }

    private UsuarioSucursal verificarAccesoSucursal(UsuarioGlobal usuario, EmpresaSucursal sucursal) {
        return usuario.getUsuarioEmpresas().stream()
            .filter(ue -> ue.getEmpresa().getId().equals(sucursal.getEmpresa().getId()))
            .flatMap(ue -> ue.getUsuarioSucursales().stream())
            .filter(us -> us.getSucursal().getId().equals(sucursal.getId()))
            .filter(UsuarioSucursal::getActivo)
            .findFirst()
            .orElse(null);
    }

    private UsuarioInfo construirUsuarioInfo(UsuarioGlobal usuario) {
        return UsuarioInfo.builder()
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .nombreCompleto(usuario.getNombreCompleto())
            .requiereCambioPassword(usuario.getDebeCambiarPassword())
            .build();
    }

    private String determinarUrlRedirect(RolNombre rol) {
        return switch (rol) {
            case SUPER_ADMIN, ADMIN -> "/dashboard";
            case JEFE_CAJAS -> "/cajas";
            case CAJERO -> "/pos";
            case MESERO -> "/mesas";
            case COCINA -> "/cocina";
            case CONTADOR -> "/reportes";
        };
    }

    private LoginResponse construirRespuestaSinAcceso(UsuarioGlobal usuario) {
        return LoginResponse.builder()
            .tipoFlujo(TipoFlujo.SIN_ACCESO)
            .usuario(construirUsuarioInfo(usuario))
            .mensaje("No tiene accesos activos en el sistema")
            .build();
    }

    private LoginResponse procesarLoginDirectoAdmin(UsuarioGlobal usuario, UsuarioEmpresa acceso) {
        // Similar a procesarLoginOperativo pero para admins con una sola opción
        EmpresaSucursal sucursalPrincipal = acceso.getEmpresa().getSucursalPrincipal();
        if (sucursalPrincipal == null) {
            sucursalPrincipal = acceso.getSucursalesConAcceso().stream()
                .filter(EmpresaSucursal::getActiva)
                .findFirst()
                .orElse(null);
        }

        if (sucursalPrincipal == null) {
            return construirRespuestaSelector(usuario, List.of(acceso));
        }

        // Generar token con contexto
        String accessToken = tokenProvider.generateTokenWithFullContext(
            usuario.getEmail(),
            acceso.getEmpresa().getId(),
            sucursalPrincipal.getId(),
            sucursalPrincipal.getSchemaName(),
            acceso.getRol().name()
        );

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(tokenProvider.generateRefreshToken(usuario.getEmail()))
            .tokenType("Bearer")
            .expiresIn(jwtExpiration)
            .tipoFlujo(TipoFlujo.DIRECTO_POS)
            .sucursalDirecta(SucursalDirecta.builder()
                .empresaId(acceso.getEmpresa().getId())
                .empresaNombre(acceso.getEmpresa().getNombreMostrar())
                .sucursalId(sucursalPrincipal.getId())
                .sucursalNombre(sucursalPrincipal.getNombreSucursal())
                .schemaName(sucursalPrincipal.getSchemaName())
                .rol(acceso.getRol().name())
                .urlRedirect(determinarUrlRedirect(acceso.getRol()))
                .build())
            .usuario(construirUsuarioInfo(usuario))
            .build();
    }
}