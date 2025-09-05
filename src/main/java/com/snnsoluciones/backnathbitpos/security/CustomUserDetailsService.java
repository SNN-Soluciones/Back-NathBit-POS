package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Intentando autenticar con: {}", usernameOrEmail);

        // Buscar primero por username, luego por email
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(usernameOrEmail)
            .or(() -> usuarioRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail));

        log.debug("Usuario encontrado: {} - {}", usuario.getUsername(), usuario.getEmail());

        return User.builder()
            .username(usuario.getEmail()) // Spring Security necesita un identificador único
            .password(usuario.getPassword())
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())
            ))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!usuario.getActivo())
            .build();
    }
}