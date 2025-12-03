package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad Tenant - Registro maestro de tenants del sistema multi-tenant.
 * Cada tenant representa una empresa/cliente con su propio schema en PostgreSQL.
 */
@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"superAdminTenants", "dispositivos"})
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del tenant (ej: "inversiones_jr")
     * Formato: lowercase, solo a-z, 0-9, underscore
     */
    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    /**
     * Nombre descriptivo del tenant
     */
    @Column(nullable = false, length = 200)
    private String nombre;

    /**
     * Nombre del schema en PostgreSQL (ej: "tenant_inversiones_jr")
     */
    @Column(name = "schema_name", unique = true, nullable = false, length = 100)
    private String schemaName;

    /**
     * Referencia a la empresa en el sistema legacy (para migración)
     */
    @Column(name = "empresa_legacy_id", unique = true)
    private Long empresaLegacyId;

    /**
     * Estado del tenant
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Configuraciones adicionales en formato JSON
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    // ==================== Auditoría ====================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Relaciones ====================

    /**
     * SUPER_ADMINs asignados a este tenant
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SuperAdminTenant> superAdminTenants = new HashSet<>();

    /**
     * Dispositivos registrados en este tenant
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Dispositivo> dispositivos = new HashSet<>();

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Generar schemaName si no está definido
        if (schemaName == null && codigo != null) {
            schemaName = "tenant_" + codigo;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Métodos de utilidad ====================

    /**
     * Verifica si el tenant está activo
     */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Verifica si el tenant fue migrado desde el sistema legacy
     */
    public boolean esMigradoDeLegacy() {
        return empresaLegacyId != null;
    }

    /**
     * Obtiene una configuración específica del JSON
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, Class<T> type) {
        if (config == null) return null;
        Object value = config.get(key);
        if (value == null) return null;
        return type.cast(value);
    }

    /**
     * Establece una configuración en el JSON
     */
    public void setConfigValue(String key, Object value) {
        if (config == null) {
            config = new HashMap<>();
        }
        config.put(key, value);
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
        Tenant tenant = (Tenant) o;
        return getId() != null && Objects.equals(getId(), tenant.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
            : getClass().hashCode();
    }
}
