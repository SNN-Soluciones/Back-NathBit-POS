package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.dto.auth.*;
import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.exception.NotFoundException;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
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
        // Autenticar usuario
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Usuario usuario = usuarioService.buscarPorUsername(request.getEmail())
            .or(() -> usuarioService.buscarPorEmail(request.getEmail()))
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar tokens
        String token = tokenProvider.generateToken(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name()
        );

        String refreshToken = tokenProvider.generateRefreshToken(
            usuario.getId(),
            usuario.getEmail()
        );

        // Preparar respuesta base
        LoginResponse.LoginResponseBuilder responseBuilder = LoginResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .usuario(UsuarioResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .build());

        // Manejo según rol
        switch (usuario.getRol()) {
            case ROOT:
            case SOPORTE:
                // ROOT y SOPORTE no requieren selección
                return responseBuilder
                    .requiereSeleccion(false)
                    .rutaDestino("/dashboard-sistema")
                    .build();

            case SUPER_ADMIN:
                // SUPER_ADMIN ve todas sus empresas
                List<UsuarioEmpresa> empresasAsignadas = usuarioEmpresaService.listarPorUsuario(usuario.getId());

                List<EmpresaResumen> empresas = empresasAsignadas.stream()
                    .map(ue -> EmpresaResumen.builder()
                        .id(ue.getEmpresa().getId())
                        .nombre(ue.getEmpresa().getNombreRazonSocial())
                        .nombreComercial(ue.getEmpresa().getNombreComercial())
                        .email(ue.getEmpresa().getEmail())
                        .identificacion(ue.getEmpresa().getIdentificacion())
                        .logo(ue.getEmpresa().getLogoUrl())
                        .requiereHacienda(ue.getEmpresa().getRequiereHacienda())
                        .activa(ue.getEmpresa().getActiva())
                        .build())
                    .collect(Collectors.toList());

                return responseBuilder
                    .empresas(empresas)
                    .requiereSeleccion(true)
                    .rutaDestino("/dashboard-empresarial")
                    .build();

            case ADMIN:
                // ADMIN tiene empresa fija pero múltiples sucursales
                List<UsuarioEmpresa> asignacionesAdmin = usuarioEmpresaService.listarPorUsuario(usuario.getId());

                if (asignacionesAdmin.isEmpty()) {
                    throw new RuntimeException("Admin sin empresa asignada");
                }

                // ADMIN solo tiene una empresa
                UsuarioEmpresa asignacionEmpresa = asignacionesAdmin.get(0);
                EmpresaResumen empresaAdmin = EmpresaResumen.builder()
                    .id(asignacionEmpresa.getEmpresa().getId())
                    .nombre(asignacionEmpresa.getEmpresa().getNombreRazonSocial())
                    .nombreComercial(asignacionEmpresa.getEmpresa().getNombreComercial())
                    .email(asignacionEmpresa.getEmpresa().getEmail())
                    .identificacion(asignacionEmpresa.getEmpresa().getIdentificacion())
                    .requiereHacienda(asignacionEmpresa.getEmpresa().getRequiereHacienda())
                    .logo(asignacionEmpresa.getEmpresa().getLogoUrl())
                    .activa(asignacionEmpresa.getEmpresa().getActiva())
                    .build();

                // Obtener sucursales asignadas al ADMIN
                List<SucursalResumen> sucursalesAdmin = usuario.getUsuarioSucursales().stream()
                    .filter(us -> us.getActivo())
                    .map(us -> new SucursalResumen(
                        us.getSucursal().getId(),
                        us.getSucursal().getNombre(),
                        us.getSucursal().getNumeroSucursal(),
                        us.getSucursal().getModoFacturacion(),
                        us.getSucursal().getActiva()
                    ))
                    .collect(Collectors.toList());

                return responseBuilder
                    .empresa(empresaAdmin)
                    .sucursales(sucursalesAdmin)
                    .requiereSeleccion(true)
                    .rutaDestino("/dashboard-sucursales/")
                    .build();

            case CAJERO:
            case MESERO:
            case COCINA:
            case JEFE_CAJAS:
                // OPERATIVOS - Contexto fijo (empresa y sucursal)
                List<UsuarioEmpresa> asignacionesOp = usuarioEmpresaService.listarPorUsuario(usuario.getId());

                if (asignacionesOp.isEmpty()) {
                    throw new RuntimeException("Operativo sin asignación");
                }

                UsuarioEmpresa asignacionOp = asignacionesOp.get(0);

                // Verificar que tenga sucursal asignada
                if (asignacionOp.getSucursal() == null) {
                    throw new RuntimeException("Operativo sin sucursal asignada");
                }

                Contexto contextoOp = Contexto.builder()
                    .empresa(EmpresaResumen.builder()
                        .id(asignacionOp.getEmpresa().getId())
                        .nombre(asignacionOp.getEmpresa().getNombreRazonSocial())
                        .nombreComercial(asignacionOp.getEmpresa().getNombreComercial())
                        .email(asignacionOp.getEmpresa().getEmail())
                        .identificacion(asignacionOp.getEmpresa().getIdentificacion())
                        .requiereHacienda(asignacionOp.getEmpresa().getRequiereHacienda())
                        .logo(asignacionOp.getEmpresa().getLogoUrl())
                        .activa(asignacionOp.getEmpresa().getActiva())
                        .build())
                    .sucursal(new SucursalResumen(
                        asignacionOp.getSucursal().getId(),
                        asignacionOp.getSucursal().getNombre(),
                        asignacionOp.getSucursal().getNumeroSucursal(),
                        asignacionOp.getSucursal().getModoFacturacion(),
                        asignacionOp.getSucursal().getActiva()
                    ))
                    .build();

                return responseBuilder
                    .contexto(contextoOp)
                    .requiereSeleccion(false)
                    .rutaDestino("/pos")
                    .build();

            default:
                throw new RuntimeException("Rol no configurado: " + usuario.getRol());
        }
    }

    @Override
    public TokenResponse establecerContexto(ContextoRequest request) {
        Long userId = getCurrentUserId();
        Usuario usuario = usuarioService.buscarPorId(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Validar acceso según rol
        switch (usuario.getRol()) {
            case ROOT:
            case SOPORTE:
                // Pueden establecer cualquier contexto
                break;

            case SUPER_ADMIN:
                // Verificar que la empresa sea suya
                boolean tieneAcceso = usuarioEmpresaService.listarPorUsuario(userId).stream()
                    .anyMatch(ue -> ue.getEmpresa().getId().equals(request.getEmpresaId()));

                if (!tieneAcceso) {
                    throw new RuntimeException("No tienes acceso a esta empresa");
                }
                break;

            case ADMIN:
                // Verificar empresa y sucursal
                List<UsuarioEmpresa> asignaciones = usuarioEmpresaService.listarPorUsuario(userId);
                if (asignaciones.isEmpty() || !asignaciones.get(0).getEmpresa().getId().equals(request.getEmpresaId())) {
                    throw new RuntimeException("No tienes acceso a esta empresa");
                }

                if (request.getSucursalId() != null) {
                    boolean tieneSucursal = usuario.getUsuarioSucursales().stream()
                        .anyMatch(us ->
                            us.getSucursal().getId().equals(request.getSucursalId()) && us.getActivo());

                    if (!tieneSucursal) {
                        throw new RuntimeException("No tienes acceso a esta sucursal");
                    }
                }
                break;

            default:
                // Operativos no pueden cambiar contexto
                throw new RuntimeException("No tienes permisos para cambiar el contexto");
        }

        // Generar nuevo token con contexto establecido
        String newToken = tokenProvider.generateTokenWithContext(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name(),
            request.getEmpresaId(),
            request.getSucursalId()
        );

        return TokenResponse.builder()
            .token(newToken)
            .build();
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        String email = tokenProvider.getEmailFromToken(refreshToken);
        String rol = tokenProvider.getRolFromToken(refreshToken);

        String newToken = tokenProvider.generateToken(userId, email, rol);

        return TokenResponse.builder()
            .token(newToken)
            .build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof ContextoUsuario contexto) {
          return contexto.getUserId();
        }

        throw new RuntimeException("No se pudo obtener el usuario actual");
    }
}