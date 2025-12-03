package com.snnsoluciones.backnathbitpos.entity.global;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad CodigoRegistro - Códigos OTP temporales para registro de dispositivos.
 * Expira en 10 minutos desde su creación.
 */
@Entity
@Table(name = "codigos_registro", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"tenant"})
public class CodigoRegistro {

    /**
     * Tiempo de expiración en minutos
     */
    public static final int MINUTOS_EXPIRACION = 10;

    /**
     * Longitud del código OTP
     */
    public static final int LONGITUD_CODIGO = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Relación ====================

    /**
     * Tenant para el cual se solicita el registro
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // ==================== Código OTP ====================

    /**
     * Nombre del dispositivo que solicita registro
     */
    @Column(name = "dispositivo_nombre", nullable = false, length = 100)
    private String dispositivoNombre;

    /**
     * Código OTP de 6 dígitos
     */
    @Column(nullable = false, length = 6)
    private String codigo;

    // ==================== Metadata ====================

    /**
     * IP desde donde se solicitó el código
     */
    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    /**
     * User-Agent del solicitante
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // ==================== Expiración ====================

    /**
     * Fecha/hora de expiración (created_at + 10 minutos)
     */
    @Column(name = "expira_at", nullable = false)
    private LocalDateTime expiraAt;

    // ==================== Estado ====================

    /**
     * Indica si el código ya fue usado
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean usado = false;

    /**
     * Fecha/hora en que se usó el código
     */
    @Column(name = "usado_at")
    private LocalDateTime usadoAt;

    // ==================== Auditoría ====================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        
        // Establecer expiración si no está definida
        if (expiraAt == null) {
            expiraAt = createdAt.plusMinutes(MINUTOS_EXPIRACION);
        }
        
        // Generar código si no existe
        if (codigo == null) {
            codigo = generarCodigo();
        }
    }

    // ==================== Métodos de utilidad ====================

    /**
     * Genera un código OTP de 6 dígitos
     */
    public static String generarCodigo() {
        SecureRandom random = new SecureRandom();
        int numero = random.nextInt(900000) + 100000; // 100000 a 999999
        return String.valueOf(numero);
    }

    /**
     * Verifica si el código ha expirado
     */
    public boolean haExpirado() {
        return LocalDateTime.now().isAfter(expiraAt);
    }

    /**
     * Verifica si el código ya fue usado
     */
    public boolean fueUsado() {
        return Boolean.TRUE.equals(usado);
    }

    /**
     * Verifica si el código es válido (no expirado y no usado)
     */
    public boolean esValido() {
        return !haExpirado() && !fueUsado();
    }

    /**
     * Marca el código como usado
     */
    public void marcarComoUsado() {
        this.usado = true;
        this.usadoAt = LocalDateTime.now();
    }

    /**
     * Obtiene los segundos restantes hasta expiración
     */
    public long segundosRestantes() {
        if (haExpirado()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiraAt).getSeconds();
    }

    /**
     * Verifica si el código proporcionado coincide
     */
    public boolean coincideCodigo(String codigoIngresado) {
        return codigo != null && codigo.equals(codigoIngresado);
    }

    // ==================== Factory Method ====================

    /**
     * Crea un nuevo código de registro para un tenant
     */
    public static CodigoRegistro crear(Tenant tenant, String dispositivoNombre, 
                                        String ipSolicitante, String userAgent) {
        LocalDateTime ahora = LocalDateTime.now();
        return CodigoRegistro.builder()
            .tenant(tenant)
            .dispositivoNombre(dispositivoNombre)
            .codigo(generarCodigo())
            .ipSolicitante(ipSolicitante)
            .userAgent(userAgent)
            .expiraAt(ahora.plusMinutes(MINUTOS_EXPIRACION))
            .usado(false)
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
        CodigoRegistro that = (CodigoRegistro) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy 
            ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() 
            : getClass().hashCode();
    }
}