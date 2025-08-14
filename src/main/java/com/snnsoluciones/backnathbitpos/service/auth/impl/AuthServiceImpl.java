package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaService;
    private final JwtTokenProvider tokenProvider;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // Autenticar
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Obtener usuario
        Usuario usuario = usuarioService.buscarPorEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Generar token básico
        String token = tokenProvider.generateToken(
            usuario.getId(), 
            usuario.getEmail(), 
            usuario.getRol().name()
        );
        
        // Construir response según rol
        LoginResponse.LoginResponseBuilder responseBuilder = LoginResponse.builder()
            .token(token)
            .usuario(convertirUsuario(usuario));
        
        // ROOT/SOPORTE - Acceso directo
        if (usuario.esRolSistema()) {
            return responseBuilder
                .requiereSeleccion(false)
                .rutaDestino("/dashboard-sistema")
                .build();
        }
        
        // SUPER_ADMIN - Lista sus empresas
        if (usuario.esRolEmpresarial()) {
            List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(usuario.getId());
            List<EmpresaResumen> empresas = asignaciones.stream()
                .map(ue -> new EmpresaResumen(
                    ue.getEmpresa().getId(),
                    ue.getEmpresa().getNombreRazonSocial()
                ))
                .distinct()
                .collect(Collectors.toList());
            
            return responseBuilder
                .empresas(empresas)
                .requiereSeleccion(true)
                .rutaDestino("/dashboard-empresarial")
                .build();
        }
        
        // ADMIN - Una empresa, múltiples sucursales
        if (usuario.esRolGerencial()) {
            List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(usuario.getId());
            if (!asignaciones.isEmpty()) {
                UsuarioEmpresa primeraAsignacion = asignaciones.get(0);
                EmpresaResumen empresa = new EmpresaResumen(
                    primeraAsignacion.getEmpresa().getId(),
                    primeraAsignacion.getEmpresa().getNombreRazonSocial()
                );
                
                List<SucursalResumen> sucursales = asignaciones.stream()
                    .filter(ue -> ue.getSucursal() != null)
                    .map(ue -> new SucursalResumen(
                        ue.getSucursal().getId(),
                        ue.getSucursal().getNombre()
                    ))
                    .collect(Collectors.toList());
                
                return responseBuilder
                    .empresa(empresa)
                    .sucursales(sucursales)
                    .requiereSeleccion(true)
                    .rutaDestino("/dashboard-empresa")
                    .build();
            }
        }
        
        // OPERATIVOS - Contexto fijo
        if (usuario.esRolOperativo()) {
            List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(usuario.getId());
            if (!asignaciones.isEmpty()) {
                UsuarioEmpresa asignacion = asignaciones.get(0);
                
                Contexto contexto = Contexto.builder()
                    .empresa(new EmpresaResumen(
                        asignacion.getEmpresa().getId(),
                        asignacion.getEmpresa().getNombreRazonSocial()))
                    .sucursal(asignacion.getSucursal() != null ? 
                        new SucursalResumen(
                            asignacion.getSucursal().getId(),
                            asignacion.getSucursal().getNombre()
                        ) : null)
                    .build();
                
                // Generar token con contexto
                String tokenConContexto = tokenProvider.generateToken(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getRol().name()
                );
                
                return responseBuilder
                    .token(tokenConContexto)
                    .contexto(contexto)
                    .requiereSeleccion(false)
                    .rutaDestino("/pos")
                    .build();
            }
        }
        
        throw new RuntimeException("Usuario sin asignaciones");
    }
    
    @Override
    public TokenResponse establecerContexto(ContextoRequest request) {
        Long userId = getCurrentUserId();
        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // ROOT/SOPORTE pueden acceder a cualquier contexto
        if (!usuario.esRolSistema()) {
            // Validar que el usuario tiene acceso
            if (!usuarioEmpresaService.tieneAcceso(userId, request.getEmpresaId(), request.getSucursalId())) {
                throw new RuntimeException("No tiene acceso a esta empresa/sucursal");
            }
        }
        
        // Generar nuevo token con contexto
        String nuevoToken = tokenProvider.generateToken(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name()
        );
        
        return TokenResponse.builder()
            .token(nuevoToken)
            .build();
    }
    
    @Override
    public TokenResponse refresh(String refreshToken) {
        // Validar refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido");
        }
        
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Generar nuevo token
        String nuevoToken = tokenProvider.generateToken(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name()
        );
        
        return TokenResponse.builder()
            .token(nuevoToken)
            .build();
    }
    
    private UsuarioResponse convertirUsuario(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setEmail(usuario.getEmail());
        response.setNombre(usuario.getNombre());
        response.setApellidos(usuario.getApellidos());
        response.setRol(usuario.getRol());
        response.setActivo(usuario.getActivo());
        return response;
    }
    
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}