package com.snnsoluciones.backnathbitpos.service.auth.multitenant;

import com.snnsoluciones.backnathbitpos.dto.auth.multitenant.AuthMultitenantDTOs.*;
import com.snnsoluciones.backnathbitpos.entity.global.SuperAdminTenant;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.UnauthorizedException;
import com.snnsoluciones.backnathbitpos.repository.global.SuperAdminTenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.TenantRepository;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación para usuarios globales (ROOT, SOPORTE, SUPER_ADMIN).
 * Estos usuarios se autentican con email y password, no con PIN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthGlobalService {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final SuperAdminTenantRepository superAdminTenantRepository;
    private final TenantRepository tenantRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Realiza el login de un usuario global.
     * 
     * @param request Credenciales del usuario
     * @return Response con token y datos del usuario
     */
    public LoginGlobalResponse login(LoginGlobalRequest request) {
        log.info("Intento de login global: {}", request.getEmail());

        // Buscar usuario por email
        UsuarioGlobal usuario = usuarioGlobalRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        // Verificar que esté activo
        if (!usuario.estaActivo()) {
            log.warn("Usuario inactivo intentó login: {}", request.getEmail());
            throw new UnauthorizedException("Usuario desactivado");
        }

        // Verificar password
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("Password incorrecto para: {}", request.getEmail());
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // Registrar acceso
        usuario.registrarAcceso();
        usuarioGlobalRepository.save(usuario);

        // Obtener tenants asignados (solo para SUPER_ADMIN)
        List<TenantResumen> tenants = obtenerTenantsDeUsuario(usuario);

        // Generar token
        Long empresaLegacyId = null;
        if (!tenants.isEmpty()) {
            empresaLegacyId = tenantRepository
                .findById(tenants.get(0).getId())
                .map(Tenant::getEmpresaLegacyId)
                .orElse(null);
        }

        String token = jwtTokenProvider.generateTokenWithContext(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name(),
            empresaLegacyId,
            null
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
            usuario.getId(),
            usuario.getEmail()
        );

        log.info("Login exitoso para usuario global: {} ({})", usuario.getEmail(), usuario.getRol());

        return LoginGlobalResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .usuario(mapToUsuarioGlobalInfo(usuario))
            .tenants(tenants)
            .requiereSeleccionTenant(usuario.esSuperAdmin() && tenants.size() > 1)
            .build();
    }

    /**
     * Obtiene los tenants asignados a un usuario.
     * ROOT y SOPORTE ven todos los tenants.
     * SUPER_ADMIN ve solo los asignados.
     */
    @Transactional(readOnly = true)
    public List<TenantResumen> obtenerTenantsDeUsuario(UsuarioGlobal usuario) {
        if (usuario.esRolSistema()) {
            // ROOT y SOPORTE ven todos los tenants activos
            return tenantRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(t -> TenantResumen.builder()
                    .id(t.getId())
                    .codigo(t.getCodigo())
                    .nombre(t.getNombre())
                    .esPropietario(false) // No aplica para ROOT/SOPORTE
                    .build())
                .collect(Collectors.toList());
        }

        if (usuario.esSuperAdmin()) {
            // SUPER_ADMIN ve solo sus tenants
            List<SuperAdminTenant> relaciones = superAdminTenantRepository
                .findByUsuarioIdAndActivoTrue(usuario.getId());

            return relaciones.stream()
                .filter(r -> r.getTenant().estaActivo())
                .map(r -> TenantResumen.builder()
                    .id(r.getTenant().getId())
                    .codigo(r.getTenant().getCodigo())
                    .nombre(r.getTenant().getNombre())
                    .esPropietario(r.esPropietario())
                    .build())
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Genera un token con contexto de tenant específico.
     * Usado cuando SUPER_ADMIN selecciona un tenant.
     */
    public String generarTokenConTenant(Long usuarioId, Long tenantId) {
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioId)
            .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new BadRequestException("Tenant no encontrado"));

        // Verificar acceso
        if (!tieneAccesoATenant(usuario, tenantId)) {
            throw new UnauthorizedException("No tiene acceso a este tenant");
        }

        // Usar empresaId para compatibilidad con sistema legacy
        // Si el tenant tiene empresa_legacy_id, usamos ese. Si no, usamos el tenant_id negativo
        Long empresaIdCompatible = tenant.getEmpresaLegacyId() != null 
            ? tenant.getEmpresaLegacyId() 
            : -tenant.getId(); // Negativo para distinguir de IDs legacy

        return jwtTokenProvider.generateTokenWithContext(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getRol().name(),
            empresaIdCompatible,
            null // Sin sucursal por defecto
        );
    }

    /**
     * Verifica si un usuario tiene acceso a un tenant.
     */
    @Transactional(readOnly = true)
    public boolean tieneAccesoATenant(UsuarioGlobal usuario, Long tenantId) {
        if (usuario.esRolSistema()) {
            return true;
        }
        return superAdminTenantRepository.existsActiveByUsuarioIdAndTenantId(
            usuario.getId(), tenantId
        );
    }

    /**
     * Cambia la contraseña de un usuario global.
     */
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        UsuarioGlobal usuario = usuarioGlobalRepository.findById(usuarioId)
            .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        // Verificar password actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new BadRequestException("Contraseña actual incorrecta");
        }

        // Validar nueva contraseña
        if (passwordNueva.length() < 8) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres");
        }

        // Actualizar
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuario.setRequiereCambioPassword(false);
        usuarioGlobalRepository.save(usuario);

        log.info("Password cambiado para usuario global: {}", usuario.getEmail());
    }

    // ==================== Mappers ====================

    private UsuarioGlobalInfo mapToUsuarioGlobalInfo(UsuarioGlobal usuario) {
        return UsuarioGlobalInfo.builder()
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .nombreCompleto(usuario.getNombreCompleto())
            .rol(usuario.getRol().name())
            .requiereCambioPassword(Boolean.TRUE.equals(usuario.getRequiereCambioPassword()))
            .build();
    }
}
