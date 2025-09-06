package com.snnsoluciones.backnathbitpos.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación custom de UserDetails para incluir información adicional
 * Parte del sistema de seguridad La Jachuda 🚀
 */
@Getter
public class CustomUserDetails implements UserDetails {
    
    private final Long id;
    private final String username;
    private final String password;
    private final String nombre;
    private final String apellidos;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean activo;
    private final Usuario usuario;
    
    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getEmail();
        this.password = usuario.getPassword();
        this.nombre = usuario.getNombre();
        this.apellidos = usuario.getApellidos();
        this.activo = usuario.getActivo();
        this.usuario = usuario;
        
        // Convertir rol a authorities
        this.authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())
        );
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return activo;
    }
    
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}