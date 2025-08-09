package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Servicio personalizado para cargar usuarios durante la autenticación.
 * Implementa UserDetailsService de Spring Security.
 */
@Service("customUserDetailsService")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga un usuario por su email o username.
     * Este método es llamado por Spring Security durante el proceso de autenticación.
     *
     * @param username el email o username del usuario
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Cargando usuario: {}", username);

        // Buscar por email o username
        Usuario usuario = usuarioRepository.findByEmailOrUsername(username, username)
            .orElseThrow(() -> {
                log.error("Usuario no encontrado: {}", username);
                return new UsernameNotFoundException("Usuario no encontrado: " + username);
            });

        // Validar estado del usuario
        validarEstadoUsuario(usuario);

        // Crear authorities basadas en el rol global del usuario
        Collection<GrantedAuthority> authorities = obtenerAuthorities(usuario);

        // Construir UserDetails
        return User.builder()
            .username(usuario.getEmail()) // Usamos email como username principal
            .password(usuario.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(usuario.getBloqueado())
            .credentialsExpired(usuario.getPasswordTemporal() != null && usuario.getPasswordTemporal())
            .disabled(!usuario.getActivo())
            .build();
    }

    /**
     * Método alternativo para cargar usuario por ID.
     * Útil cuando el JWT contiene el ID del usuario.
     *
     * @param userId ID del usuario
     * @return UserDetails con la información del usuario
     */
    @Transactional
    public UserDetails loadUserById(Long userId) {
        log.debug("Cargando usuario por ID: {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("Usuario no encontrado con ID: {}", userId);
                return new UsernameNotFoundException("Usuario no encontrado con ID: " + userId);
            });

        // Validar estado del usuario
        validarEstadoUsuario(usuario);

        // Crear authorities
        Collection<GrantedAuthority> authorities = obtenerAuthorities(usuario);

        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(usuario.getBloqueado())
            .credentialsExpired(usuario.getPasswordTemporal() != null && usuario.getPasswordTemporal())
            .disabled(!usuario.getActivo())
            .build();
    }

    /**
     * Obtiene las authorities (roles) del usuario basado en su rol global
     */
    private Collection<GrantedAuthority> obtenerAuthorities(Usuario usuario) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Agregar rol principal con prefijo ROLE_
        authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

        // Agregar authorities adicionales según el tipo de rol
        if (usuario.esRolSistema()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SISTEMA"));

            // Permisos específicos para usuarios del sistema
            authorities.add(new SimpleGrantedAuthority("PERM_ACCESO_TOTAL"));
            authorities.add(new SimpleGrantedAuthority("PERM_VER_TODAS_EMPRESAS"));
            authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_SISTEMA"));
        } else if (usuario.esRolAdministrativo()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATIVO"));

            // Permisos para administrativos
            authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_USUARIOS"));
            authorities.add(new SimpleGrantedAuthority("PERM_VER_REPORTES"));
            authorities.add(new SimpleGrantedAuthority("PERM_CONFIGURAR_EMPRESA"));
        } else if (usuario.esRolOperativo()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATIVO"));

            // Permisos básicos para operativos
            authorities.add(new SimpleGrantedAuthority("PERM_USAR_POS"));

            // Permisos específicos por rol operativo
            switch (usuario.getRol()) {
                case JEFE_CAJAS:
                    authorities.add(new SimpleGrantedAuthority("PERM_SUPERVISAR_CAJAS"));
                    authorities.add(new SimpleGrantedAuthority("PERM_VER_REPORTES_CAJA"));
                    break;
                case CAJERO:
                    authorities.add(new SimpleGrantedAuthority("PERM_OPERAR_CAJA"));
                    authorities.add(new SimpleGrantedAuthority("PERM_FACTURAR"));
                    break;
                case MESERO:
                    authorities.add(new SimpleGrantedAuthority("PERM_TOMAR_ORDENES"));
                    authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_MESAS"));
                    break;
                case COCINA:
                    authorities.add(new SimpleGrantedAuthority("PERM_VER_ORDENES_COCINA"));
                    break;
            }
        }

        // Authority genérica para todos los usuarios autenticados
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        log.debug("Authorities asignadas para usuario {}: {}", usuario.getEmail(), authorities);
        return authorities;
    }

    /**
     * Valida el estado del usuario antes de permitir el login
     */
    private void validarEstadoUsuario(Usuario usuario) {
        if (!usuario.getActivo()) {
            log.warn("Intento de login con usuario inactivo: {}", usuario.getEmail());
            throw new UsernameNotFoundException("Usuario inactivo");
        }

        if (usuario.getBloqueado()) {
            // Verificar si ya puede ser desbloqueado
            if (usuario.getFechaDesbloqueo() != null &&
                usuario.getFechaDesbloqueo().isBefore(LocalDateTime.now())) {
                log.info("Desbloqueando usuario automáticamente: {}", usuario.getEmail());
                // El servicio de autenticación se encargará de actualizar el estado
            } else {
                log.warn("Intento de login con usuario bloqueado: {}", usuario.getEmail());
                throw new UsernameNotFoundException("Usuario bloqueado");
            }
        }
    }

    /**
     * Carga un usuario con todas sus relaciones (empresas/sucursales)
     * Útil para operaciones que requieren el contexto completo
     */
    @Transactional
    public Usuario loadUsuarioCompleto(String username) {
        return usuarioRepository.findByEmailWithEmpresas(username)
            .orElseGet(() -> usuarioRepository.findByIdWithEmpresas(
                Long.parseLong(username)
            ).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username)));
    }
}