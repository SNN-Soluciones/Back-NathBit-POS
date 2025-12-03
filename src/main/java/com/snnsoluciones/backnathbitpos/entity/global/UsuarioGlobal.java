package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad UsuarioGlobal - Usuarios con acceso cross-tenant.
 * Solo pueden tener roles: ROOT, SOPORTE, SUPER_ADMIN
 */
@Entity
@Table(name = "usuarios_globales", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"superAdminTenants"})
public class UsuarioGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Credenciales ====================

    /**
     * Email único (usado como username)
     */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * Password encriptado con BCrypt
     */
    @Column(nullable = false, length = 255)
    private String password;

    // ==================== Datos personales ====================

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    @Column(length = 20)
    private String telefono;

    // ==================== Rol ====================

    /**
     * Rol del usuario: ROOT, SOPORTE, SUPER_ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolGlobal rol;

    // ==================== Estado ====================

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Builder.Default
    @Column(name = "requiere_cambio_password")
    private Boolean requiereCambioPassword = false;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    // ==================== Migración Legacy ====================

    /**
     * Referencia al usuario en el sistema legacy
     */
    @Column(name = "usuario_legacy_id", unique = true)
    private Long usuarioLegacyId;

    // ==================== Auditoría ====================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Relaciones ====================

    /**
     * Tenants asignados (solo aplica para SUPER_ADMIN)
     */
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SuperAdminTenant> superAdminTenants = new HashSet<>();

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Métodos de utilidad ====================

    /**
     * Verifica si el usuario está activo
     */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Verifica si es ROOT
     */
    public boolean esRoot() {
        return rol == RolGlobal.ROOT;
    }

    /**
     * Verifica si es SOPORTE
     */
    public boolean esSoporte() {
        return rol == RolGlobal.SOPORTE;
    }

    /**
     * Verifica si es SUPER_ADMIN
     */
    public boolean esSuperAdmin() {
        return rol == RolGlobal.SUPER_ADMIN;
    }

    /**
     * Verifica si es rol de sistema (ROOT o SOPORTE)
     */
    public boolean esRolSistema() {
        return rol == RolGlobal.ROOT || rol == RolGlobal.SOPORTE;
    }

    /**
     * Verifica si fue migrado desde el sistema legacy
     */
    public boolean esMigradoDeLegacy() {
        return usuarioLegacyId != null;
    }

    /**
     * Obtiene el nombre completo
     */
    public String getNombreCompleto() {
        if (apellidos == null || apellidos.isBlank()) {
            return nombre;
        }
        return nombre + " " + apellidos;
    }

    /**
     * Registra el acceso actual
     */
    public void registrarAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }

    // ==================== Equals & HashCode ====================

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy 
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() 
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() 
            : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UsuarioGlobal that = (UsuarioGlobal) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
            : getClass().hashCode();
    }

    // ==================== Enum de Roles Globales ====================

    /**
     * Roles permitidos para usuarios globales
     */
    public enum RolGlobal {
        ROOT("Root", "Control total del sistema"),
        SOPORTE("Soporte", "Soporte técnico con acceso casi total"),
        SUPER_ADMIN("Super Admin", "Dueño de empresa/restaurante");

        private final String displayName;
        private final String descripcion;

        RolGlobal(String displayName, String descripcion) {
            this.displayName = displayName;
            this.descripcion = descripcion;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}
