package com.snnsoluciones.backnathbitpos.config.security;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
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
import java.util.*;
import java.util.stream.Collectors;

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
    private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;

    /**
     * Carga un usuario por su email.
     * Este método es llamado por Spring Security durante el proceso de autenticación.
     *
     * @param username el email del usuario
     * @return UserDetails con la información del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Cargando usuario: {}", username);

        // Buscar por email
        Usuario usuario = usuarioRepository.findByEmail(username)
            .orElseThrow(() -> {
                log.error("Usuario no encontrado: {}", username);
                return new UsernameNotFoundException("Usuario no encontrado: " + username);
            });

        // Validar estado del usuario
        validarEstadoUsuario(usuario);

        // Obtener roles del usuario
        List<UsuarioEmpresaRol> rolesUsuario = usuarioEmpresaRolRepository.findByUsuarioId(usuario.getId());

        // Crear authorities basadas en todos los roles del usuario
        Collection<GrantedAuthority> authorities = obtenerAuthorities(usuario, rolesUsuario);

        // Construir UserDetails
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

        // Obtener roles del usuario
        List<UsuarioEmpresaRol> rolesUsuario = usuarioEmpresaRolRepository.findByUsuarioId(userId);

        // Crear authorities
        Collection<GrantedAuthority> authorities = obtenerAuthorities(usuario, rolesUsuario);

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
     * Obtiene las authorities (roles) del usuario basado en todos sus roles asignados
     */
    private Collection<GrantedAuthority> obtenerAuthorities(Usuario usuario, List<UsuarioEmpresaRol> rolesUsuario) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Si no tiene roles asignados, error
        if (rolesUsuario.isEmpty()) {
            log.warn("Usuario {} sin roles asignados", usuario.getEmail());
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            return authorities;
        }

        // Obtener todos los roles únicos del usuario
        Set<RolNombre> rolesUnicos = rolesUsuario.stream()
            .filter(uer -> uer.getActivo())
            .map(UsuarioEmpresaRol::getRol)
            .collect(Collectors.toSet());

        // Agregar authorities por cada rol único
        for (RolNombre rol : rolesUnicos) {
            // Agregar rol con prefijo ROLE_
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));

            // Agregar authorities adicionales según el tipo de rol
            agregarAuthoritiesPorRol(rol, authorities);
        }

        // Agregar categorías de roles si aplica
        if (rolesUnicos.stream().anyMatch(this::esRolSistema)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SISTEMA"));
        }
        if (rolesUnicos.stream().anyMatch(this::esRolAdministrativo)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATIVO"));
        }
        if (rolesUnicos.stream().anyMatch(this::esRolOperativo)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_OPERATIVO"));
        }

        // Authority genérica para todos los usuarios autenticados
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        log.debug("Authorities asignadas para usuario {}: {}", usuario.getEmail(), authorities);
        return authorities;
    }

    /**
     * Agrega authorities específicas según el rol
     */
    private void agregarAuthoritiesPorRol(RolNombre rol, Set<GrantedAuthority> authorities) {
        switch (rol) {
            case ROOT:
            case SOPORTE:
                authorities.add(new SimpleGrantedAuthority("PERM_ACCESO_TOTAL"));
                authorities.add(new SimpleGrantedAuthority("PERM_VER_TODAS_EMPRESAS"));
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_SISTEMA"));
                authorities.add(new SimpleGrantedAuthority("PERM_BACKUP_RESTORE"));
                break;

            case SUPER_ADMIN:
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_EMPRESAS"));
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_USUARIOS"));
                authorities.add(new SimpleGrantedAuthority("PERM_VER_REPORTES_GLOBALES"));
                authorities.add(new SimpleGrantedAuthority("PERM_CONFIGURAR_SISTEMA"));
                break;

            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_USUARIOS"));
                authorities.add(new SimpleGrantedAuthority("PERM_VER_REPORTES"));
                authorities.add(new SimpleGrantedAuthority("PERM_CONFIGURAR_EMPRESA"));
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_SUCURSALES"));
                break;

            case JEFE_CAJAS:
                authorities.add(new SimpleGrantedAuthority("PERM_SUPERVISAR_CAJAS"));
                authorities.add(new SimpleGrantedAuthority("PERM_VER_REPORTES_CAJA"));
                authorities.add(new SimpleGrantedAuthority("PERM_ANULAR_OPERACIONES"));
                authorities.add(new SimpleGrantedAuthority("PERM_USAR_POS"));
                break;

            case CAJERO:
                authorities.add(new SimpleGrantedAuthority("PERM_USAR_POS"));
                authorities.add(new SimpleGrantedAuthority("PERM_OPERAR_CAJA"));
                authorities.add(new SimpleGrantedAuthority("PERM_FACTURAR"));
                authorities.add(new SimpleGrantedAuthority("PERM_COBRAR"));
                break;

            case MESERO:
                authorities.add(new SimpleGrantedAuthority("PERM_TOMAR_ORDENES"));
                authorities.add(new SimpleGrantedAuthority("PERM_GESTIONAR_MESAS"));
                authorities.add(new SimpleGrantedAuthority("PERM_VER_ESTADO_ORDENES"));
                break;

            case COCINA:
                authorities.add(new SimpleGrantedAuthority("PERM_VER_ORDENES_COCINA"));
                authorities.add(new SimpleGrantedAuthority("PERM_ACTUALIZAR_ESTADO_ORDENES"));
                break;

            case TEMPORAL:
                authorities.add(new SimpleGrantedAuthority("PERM_ACCESO_LIMITADO"));
                break;
        }
    }

    /**
     * Verifica si es un rol del sistema
     */
    private boolean esRolSistema(RolNombre rol) {
        return rol == RolNombre.ROOT || rol == RolNombre.SOPORTE;
    }

    /**
     * Verifica si es un rol administrativo
     */
    private boolean esRolAdministrativo(RolNombre rol) {
        return rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN;
    }

    /**
     * Verifica si es un rol operativo
     */
    private boolean esRolOperativo(RolNombre rol) {
        return rol == RolNombre.JEFE_CAJAS ||
            rol == RolNombre.CAJERO ||
            rol == RolNombre.MESERO ||
            rol == RolNombre.COCINA;
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
    public Usuario loadUsuarioCompleto(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // Cargar sus roles con empresas y sucursales
        List<UsuarioEmpresaRol> rolesCompletos = usuarioEmpresaRolRepository.findByUsuarioId(usuario.getId());

        // Forzar la carga de las relaciones lazy si es necesario
        rolesCompletos.forEach(uer -> {
            // Cargar empresa
            if (uer.getEmpresa() != null) {
                uer.getEmpresa().getNombre(); // Forzar carga

                // Cargar sucursales de la empresa si tiene acceso a todas
                if (uer.getSucursal() == null) {
                    uer.getEmpresa().getSucursales().size(); // Forzar carga de sucursales
                }
            }

            // Cargar sucursal específica si existe
            if (uer.getSucursal() != null) {
                uer.getSucursal().getNombre(); // Forzar carga
            }
        });

        // Asociar los roles cargados al usuario (si tienes una relación bidireccional)
        // usuario.setUsuarioEmpresaRoles(rolesCompletos);

        return usuario;
    }
}