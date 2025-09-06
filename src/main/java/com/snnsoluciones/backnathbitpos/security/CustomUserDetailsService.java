package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para cargar usuarios para Spring Security
 * Trabaja con tu AuthService existente sin cambios
 */
@Slf4j
@Service("userDetailsService")  // Nombre explícito para evitar conflictos
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioService usuarioService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Cargando usuario para Spring Security: {}", username);

        // Usar el mismo método que tu AuthService
        Usuario usuario = usuarioService.buscarPorUsername(username)
            .or(() -> usuarioService.buscarPorEmail(username))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        // Retornar CustomUserDetails para tener info adicional disponible
        return new CustomUserDetails(usuario);
    }
}