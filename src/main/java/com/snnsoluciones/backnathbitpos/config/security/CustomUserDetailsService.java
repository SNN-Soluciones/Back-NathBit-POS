package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String tenantId = TenantContext.getCurrentTenant();

    if (tenantId == null) {
      throw new UsernameNotFoundException("No se ha establecido el tenant");
    }

    Usuario usuario = usuarioRepository.findByEmailAndTenant(email, tenantId)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado con email: " + email + " en tenant: " + tenantId
        ));

    log.debug("Usuario cargado: {} para tenant: {}", email, tenantId);

    return usuario;
  }

  /**
   * Método auxiliar para cargar usuario por email sin verificar tenant
   * Útil para super admin
   */
  @Transactional
  public UserDetails loadUserByUsernameIgnoreTenant(String email) throws UsernameNotFoundException {
    return usuarioRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado con email: " + email
        ));
  }
}