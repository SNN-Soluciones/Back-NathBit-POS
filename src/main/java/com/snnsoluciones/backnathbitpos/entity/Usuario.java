package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuarioEmpresaRoles"})
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    @Column(length = 50)
    private String identificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", length = 20)
    private TipoIdentificacion tipoIdentificacion = TipoIdentificacion.CEDULA_JURIDICA;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueado = false;

    @Column(name = "intentos_fallidos")
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_cambio_password")
    private LocalDateTime fechaCambioPassword;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // Relaciones
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UsuarioEmpresaRol> usuarioEmpresaRoles = new HashSet<>();

    // Métodos de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return usuarioEmpresaRoles.stream()
                .filter(uer -> uer.getActivo())
                .map(uer -> new SimpleGrantedAuthority("ROLE_" + uer.getRol().name()))
                .collect(Collectors.toSet());
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
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    // Métodos de utilidad
    public String getNombreCompleto() {
        if (apellidos != null && !apellidos.isEmpty()) {
            return nombre + " " + apellidos;
        }
        return nombre;
    }

    public void incrementarIntentosFallidos() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= 3) {
            this.bloqueado = true;
        }
    }

    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
        this.bloqueado = false;
    }

    public boolean tieneRolEnEmpresa(Long empresaId, RolNombre rol) {
        return usuarioEmpresaRoles.stream()
                .anyMatch(uer -> uer.getActivo() && 
                                uer.getEmpresa().getId().equals(empresaId) && 
                                uer.getRol().equals(rol));
    }

    public boolean tieneAccesoAEmpresa(Long empresaId) {
        return usuarioEmpresaRoles.stream()
                .anyMatch(uer -> uer.getActivo() && 
                                uer.getEmpresa().getId().equals(empresaId));
    }

    public boolean tieneAccesoASucursal(Long empresaId, Long sucursalId) {
        return usuarioEmpresaRoles.stream()
                .anyMatch(uer -> uer.getActivo() && 
                                uer.getEmpresa().getId().equals(empresaId) &&
                                (uer.getSucursal() == null || 
                                 uer.getSucursal().getId().equals(sucursalId)));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        Usuario usuario = (Usuario) o;
        return getId() != null && Objects.equals(getId(), usuario.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}