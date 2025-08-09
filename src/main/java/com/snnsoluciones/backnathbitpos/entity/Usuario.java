package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "username", unique = true, length = 50)
    private String username;

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
    private TipoIdentificacion tipoIdentificacion;

    // ROL ÚNICO GLOBAL - Cambio principal del modelo
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolNombre rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    @Builder.Default
    private TipoUsuario tipoUsuario = TipoUsuario.EMPRESARIAL;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bloqueado = false;

    @Column(name = "password_temporal")
    @Builder.Default
    private Boolean passwordTemporal = false;

    @Column(name = "intentos_fallidos")
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "fecha_desbloqueo")
    private LocalDateTime fechaDesbloqueo;

    @Column(length = 100)
    private String direccion;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relaciones
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<UsuarioEmpresaRol> usuarioEmpresaRoles = new HashSet<>();

    // Métodos de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Agregar rol global
        if (this.rol != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + this.rol.name()));
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return this.email; // Usamos email como username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.bloqueado;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.activo && !this.bloqueado;
    }

    // Métodos helper
    public String getNombreCompleto() {
        return String.format("%s %s", nombre, apellidos != null ? apellidos : "").trim();
    }

    public boolean esUsuarioSistema() {
        return this.tipoUsuario == TipoUsuario.SISTEMA;
    }

    public boolean esRolSistema() {
        return this.rol == RolNombre.ROOT || this.rol == RolNombre.SOPORTE;
    }

    public boolean requiereSeleccionContexto() {
        return this.rol == RolNombre.SUPER_ADMIN || this.rol == RolNombre.ADMIN;
    }

    public boolean esOperativo() {
        return this.rol == RolNombre.CAJERO ||
            this.rol == RolNombre.MESERO ||
            this.rol == RolNombre.JEFE_CAJAS ||
            this.rol == RolNombre.COCINA;
    }

    public void incrementarIntentosFallidos() {
        this.intentosFallidos = (this.intentosFallidos == null ? 0 : this.intentosFallidos) + 1;
    }

    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
        this.fechaBloqueo = null;
        this.bloqueado = false;
    }

    public void bloquearCuenta() {
        this.bloqueado = true;
        this.fechaBloqueo = LocalDateTime.now();
    }

    // equals y hashCode basados en ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}