package com.snnsoluciones.backnathbitpos.entity.global;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entidad que representa un usuario global del sistema.
 * Los usuarios están en el schema public y pueden tener acceso a múltiples empresas/sucursales.
 */
@Entity
@Table(name = "usuarios_global", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuarioEmpresas"})
@EqualsAndHashCode(exclude = {"usuarioEmpresas"})
public class UsuarioGlobal implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 50)
    private String identificacion;

    @Column(name = "tipo_identificacion", length = 20)
    @Enumerated(EnumType.STRING)
    private TipoIdentificacion tipoIdentificacion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean bloqueado = false;

    @Builder.Default
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_password_expira")
    private LocalDateTime fechaPasswordExpira;

    @Builder.Default
    @Column(name = "debe_cambiar_password")
    private Boolean debeCambiarPassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Relaciones
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UsuarioEmpresa> usuarioEmpresas = new HashSet<>();

    // Métodos de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Este método se sobrescribirá en el servicio según el contexto seleccionado
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (fechaPasswordExpira == null) {
            return true;
        }
        return fechaPasswordExpira.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    // Métodos helper
    public String getNombreCompleto() {
        return nombre + (apellidos != null ? " " + apellidos : "");
    }

    public boolean tieneAccesoAEmpresa(UUID empresaId) {
        return usuarioEmpresas.stream()
                .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo());
    }

    public List<Empresa> getEmpresasActivas() {
        return usuarioEmpresas.stream()
                .filter(UsuarioEmpresa::getActivo)
                .map(UsuarioEmpresa::getEmpresa)
                .filter(Empresa::getActiva)
                .collect(Collectors.toList());
    }

    public UsuarioEmpresa getAccesoEmpresa(UUID empresaId) {
        return usuarioEmpresas.stream()
                .filter(ue -> ue.getEmpresa().getId().equals(empresaId))
                .findFirst()
                .orElse(null);
    }

    public boolean esUsuarioOperativo() {
        // Un usuario es operativo si todos sus roles son CAJERO o MESERO
        return usuarioEmpresas.stream()
                .filter(UsuarioEmpresa::getActivo)
                .allMatch(ue -> ue.getRol() == RolNombre.CAJERO || ue.getRol() == RolNombre.MESERO);
    }

    public boolean esAdministrador() {
        return usuarioEmpresas.stream()
                .filter(UsuarioEmpresa::getActivo)
                .anyMatch(ue -> ue.getRol() == RolNombre.SUPER_ADMIN || ue.getRol() == RolNombre.ADMIN);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}