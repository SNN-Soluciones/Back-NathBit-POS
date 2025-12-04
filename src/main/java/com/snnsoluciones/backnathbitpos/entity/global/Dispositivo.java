package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Dispositivo - Dispositivos registrados para acceso al POS.
 * Cada dispositivo tiene un token único y permanente vinculado a UN solo tenant.
 */
@Entity
@Table(name = "dispositivos", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"tenant"})
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Relación ====================

    /**
     * Tenant al que pertenece el dispositivo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // ==================== Identificación ====================

    /**
     * Nombre descriptivo del dispositivo (ej: "Caja Principal", "Tablet Meseros")
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Token único y permanente del dispositivo (UUID)
     * Se genera al registrar el dispositivo y NO expira
     */
    @Column(unique = true, nullable = false, length = 255)
    private String token;

    // ==================== Metadata ====================

    /**
     * Plataforma del dispositivo: WEB, ANDROID, IOS, WINDOWS
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Plataforma plataforma;

    /**
     * User-Agent del navegador/app
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * IP desde donde se registró el dispositivo
     */
    @Column(name = "ip_registro", length = 45)
    private String ipRegistro;

    // ==================== Estado ====================

    /**
     * Estado del dispositivo
     * Si se desactiva, el dispositivo deberá re-registrarse
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Último uso del dispositivo
     */
    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;

    // ==================== Auditoría ====================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Generar token si no existe
        if (token == null) {
            token = generarToken();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== Métodos de utilidad ====================

    /**
     * Genera un token único para el dispositivo
     */
    public static String generarToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Verifica si el dispositivo está activo
     */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Registra el uso actual del dispositivo
     */
    public void registrarUso() {
        this.ultimoUso = LocalDateTime.now();
    }

    /**
     * Desactiva el dispositivo (requiere re-registro)
     */
    public void desactivar() {
        this.activo = false;
    }

    /**
     * Reactiva el dispositivo
     */
    public void activar() {
        this.activo = true;
    }

    // ==================== Factory Method ====================

    /**
     * Crea un nuevo dispositivo con token generado
     */
    public static Dispositivo crear(Tenant tenant, String nombre, Plataforma plataforma, 
                                     String userAgent, String ipRegistro) {
        return Dispositivo.builder()
            .tenant(tenant)
            .nombre(nombre)
            .token(generarToken())
            .plataforma(plataforma)
            .userAgent(userAgent)
            .ipRegistro(ipRegistro)
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
        Dispositivo that = (Dispositivo) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
            : getClass().hashCode();
    }

    // ==================== Enum de Plataformas ====================

    /**
     * Plataformas soportadas
     */
    public enum Plataforma {
        WEB("Navegador Web"),
        ANDROID("Android"),
        IOS("iOS"),
        WINDOWS("Windows Desktop");

        private final String descripcion;

        Plataforma(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }
}
