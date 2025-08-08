package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Carga un usuario por su email (username).
     * Este método es llamado por Spring Security durante el proceso de autenticación.
     *
     * @param username el email del usuario (usado como username)
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar primero por email, luego por username
        Usuario usuario = usuarioRepository.findByEmail(username)
            .orElseGet(() -> usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username)));

        // Para usuarios SISTEMA, no necesitamos cargar roles de empresa
        if (usuario.getTipoUsuario().esSistema()) {
            // Crear authorities basadas en el tipo de usuario sistema
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();

            if (usuario.getEmail().contains("root")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ROOT"));
            } else if (usuario.getEmail().contains("developer")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_DEVELOPER"));
            }

            return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(authorities)
                .accountExpired(false)          // NO accountNonExpired
                .accountLocked(usuario.getBloqueado())  // NO accountNonLocked
                .credentialsExpired(false)      // NO credentialsNonExpired
                .disabled(!usuario.getActivo())  // NO enabled
                .build();
        }

        // Para otros usuarios, cargar roles normalmente
        return usuarioRepository.findByEmailWithRoles(usuario.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    /**
     * Método alternativo para cargar usuario por ID.
     * Útil cuando el JWT contiene el ID del usuario.
     *
     * @param userId ID del usuario
     * @return UserDetails con la información del usuario
     */
    public UserDetails loadUserById(Long userId) {
        log.debug("Cargando usuario por ID: {}", userId);

        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("Usuario no encontrado con ID: {}", userId);
                return new UsernameNotFoundException("Usuario no encontrado con ID: " + userId);
            });

        if (!usuario.getActivo()) {
            log.warn("Usuario inactivo con ID: {}", userId);
            throw new UsernameNotFoundException("Usuario inactivo");
        }

        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getPassword())
            .authorities("ROLE_USER")
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!usuario.getActivo())
            .build();
    }
}