package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.config.security.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoAcceso;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import java.util.HashSet;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        
        try {
            // Autenticar credenciales
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Obtener usuario con roles
            Usuario usuario = usuarioRepository.findByEmailWithRoles(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
            
            if (!usuario.getActivo()) {
                throw new DisabledException("Usuario inactivo");
            }
            
            // Actualizar último acceso
            usuarioService.actualizarUltimoAcceso(usuario.getId());
            
            // Obtener accesos del usuario
            List<AccesoDTO> accesos = usuarioService.obtenerAccesos(usuario.getId());
            
            if (accesos.isEmpty()) {
                throw new UnauthorizedException("Usuario sin roles asignados");
            }
            
            // Determinar tipo de acceso
            TipoAcceso tipoAcceso = determinarTipoAcceso(accesos);
            
            LoginResponse response = new LoginResponse();
            response.setUsuario(usuarioMapper.toDTO(usuario));
            response.setTipoAcceso(tipoAcceso);
            
            // Si es operativo, generar token con contexto directo
            if (tipoAcceso == TipoAcceso.OPERATIVO) {
                AccesoDTO acceso = accesos.get(0);
                String token = jwtTokenProvider.generateToken(
                    usuario.getId(),
                    usuario.getEmail(),
                    acceso.getEmpresa().getId(),
                    acceso.getSucursal() != null ? acceso.getSucursal().getId() : null,
                    acceso.getRol()
                );
                
                response.setToken(token);
                response.setRefreshToken(jwtTokenProvider.generateRefreshToken(usuario.getId()));
                response.setAccesoDirecto(acceso);
                response.setRequiereSeleccion(false);
            } else {
                // Si es administrativo o múltiple, requiere selección
                response.setAccesosDisponibles(accesos);
                response.setRequiereSeleccion(true);
            }
            
            log.info("Login exitoso para usuario: {} - Tipo: {}", usuario.getEmail(), tipoAcceso);
            return response;
            
        } catch (BadCredentialsException e) {
            log.warn("Credenciales incorrectas para: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Credenciales incorrectas");
        } catch (DisabledException e) {
            log.warn("Usuario deshabilitado: {}", loginRequest.getEmail());
            throw new UnauthorizedException("Usuario deshabilitado");
        }
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