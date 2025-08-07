package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Service("userDetailsService")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UsuarioRepository usuarioRepository;
    
    /**
     * Carga un usuario por su email (username).
     * Este método es llamado por Spring Security durante el proceso de autenticación.
     * 
     * @param email el email del usuario (usado como username)
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando usuario por email: {}", email);
        
        // Buscar usuario por email
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("Usuario no encontrado con email: {}", email);
                return new UsernameNotFoundException("Usuario no encontrado: " + email);
            });
        
        // Verificar si el usuario está activo
        if (!usuario.getActivo()) {
            log.warn("Intento de login con usuario inactivo: {}", email);
            throw new UsernameNotFoundException("Usuario inactivo");
        }
        
        log.info("Usuario encontrado: {} - ID: {}", usuario.getEmail(), usuario.getId());
        
        // Crear UserDetails
        // Nota: Los roles específicos se manejan después del login en el JWT,
        // ya que dependen de la empresa/sucursal seleccionada
        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getPassword())
            .authorities("ROLE_USER") // Rol genérico para pasar autenticación básica
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
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