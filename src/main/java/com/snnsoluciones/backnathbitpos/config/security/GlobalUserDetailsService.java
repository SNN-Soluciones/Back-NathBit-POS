package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación usando el nuevo modelo de usuarios globales.
 * Reemplaza a CustomUserDetailsService.
 */
@Service("globalUserDetailsService")
@RequiredArgsConstructor
@Slf4j
public class GlobalUserDetailsService implements UserDetailsService {

    private final UsuarioGlobalRepository usuarioGlobalRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando usuario global: {}", email);

        UsuarioGlobal usuario = usuarioGlobalRepository.findByEmailWithEmpresas(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con email: " + email
                ));

        // Verificar si el usuario está activo
        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + email);
        }

        // Verificar si está bloqueado
        if (usuario.getBloqueado()) {
            throw new UsernameNotFoundException("Usuario bloqueado: " + email);
        }

        log.debug("Usuario {} cargado exitosamente con {} empresas asignadas", 
                email, usuario.getUsuarioEmpresas().size());

        return usuario;
    }

    /**
     * Verifica si un usuario tiene acceso a una empresa específica
     */
    public boolean hasAccessToEmpresa(String email, String empresaId) {
        try {
            UsuarioGlobal usuario = usuarioGlobalRepository.findByEmail(email)
                    .orElse(null);
            
            if (usuario == null) return false;
            
            return usuario.getUsuarioEmpresas().stream()
                    .anyMatch(ue -> ue.getEmpresa().getId().toString().equals(empresaId) 
                            && ue.getActivo() 
                            && ue.estaVigente());
        } catch (Exception e) {
            log.error("Error verificando acceso a empresa", e);
            return false;
        }
    }
}