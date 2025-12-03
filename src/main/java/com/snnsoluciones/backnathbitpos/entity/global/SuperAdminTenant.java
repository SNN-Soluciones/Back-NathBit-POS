package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad SuperAdminTenant - Relación N:M entre usuarios SUPER_ADMIN y sus tenants.
 * Permite que un SUPER_ADMIN tenga acceso a múltiples tenants.
 */
@Entity
@Table(name = "super_admin_tenants", schema = "public",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_super_admin_tenants",
        columnNames = {"usuario_id", "tenant_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"usuario", "tenant"})
public class SuperAdminTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Relaciones ====================

    /**
     * Usuario SUPER_ADMIN
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioGlobal usuario;

    /**
     * Tenant asignado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // ==================== Propiedades ====================

    /**
     * Indica si es el propietario principal del tenant
     * true = dueño principal
     * false = colaborador con acceso
     */
    @Builder.Default
    @Column(name = "es_propietario")
    private Boolean esPropietario = false;

    /**
     * Estado de la relación
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    // ==================== Auditoría ====================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== Métodos de utilidad ====================

    /**
     * Verifica si la relación está activa
     */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Verifica si es propietario del tenant
     */
    public boolean esPropietario() {
        return Boolean.TRUE.equals(esPropietario);
    }

    // ==================== Factory Methods ====================

    /**
     * Crea una relación de propietario
     */
    public static SuperAdminTenant crearPropietario(UsuarioGlobal usuario, Tenant tenant) {
        return SuperAdminTenant.builder()
            .usuario(usuario)
            .tenant(tenant)
            .esPropietario(true)
            .activo(true)
            .build();
    }

    /**
     * Crea una relación de colaborador
     */
    public static SuperAdminTenant crearColaborador(UsuarioGlobal usuario, Tenant tenant) {
        return SuperAdminTenant.builder()
            .usuario(usuario)
            .tenant(tenant)
            .esPropietario(false)
            .activo(true)
            .build();
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
        SuperAdminTenant that = (SuperAdminTenant) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
            : getClass().hashCode();
    }
}
