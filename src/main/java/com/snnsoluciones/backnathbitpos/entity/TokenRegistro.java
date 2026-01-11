package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token temporal para registro de dispositivos PDV.
 * 
 * Estos tokens se generan cuando un administrador solicita registrar un nuevo PDV.
 * Son de un solo uso y tienen una validez de 24 horas.
 * 
 * Flujo:
 * 1. Admin genera token (expira en 24h)
 * 2. PDV usa token para registrarse
 * 3. Token se marca como "usado"
 * 4. Se crea un Dispositivo con token permanente
 */
@Entity
@Table(name = "tokens_registro",
    indexes = {
        @Index(name = "idx_tokens_registro_token", columnList = "token"),
        @Index(name = "idx_tokens_registro_empresa", columnList = "empresa_id"),
        @Index(name = "idx_tokens_registro_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_tokens_registro_expira", columnList = "expira_en"),
        @Index(name = "idx_tokens_registro_usado", columnList = "usado")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRegistro {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Token único para el QR/Link de registro (formato: REG-xxxxx)
     * Se genera automáticamente al crear el token
     */
    @Column(nullable = false, unique = true, length = 255)
    private String token;
    
    /**
     * Empresa a la que pertenecerá el dispositivo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    /**
     * Sucursal a la que se asignará el dispositivo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    /**
     * Nombre descriptivo del dispositivo que se va a registrar
     */
    @Column(name = "nombre_dispositivo", length = 255)
    private String nombreDispositivo;
    
    /**
     * Indica si el token ya fue utilizado (un solo uso)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;
    
    /**
     * Fecha y hora de expiración del token (24 horas desde creación)
     */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;
    
    /**
     * Fecha y hora de creación del token
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Fecha y hora en que el token fue utilizado
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    /**
     * Verifica si el token está expirado
     */
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(expiraEn);
    }
    
    /**
     * Verifica si el token es válido (no usado y no expirado)
     */
    public boolean esValido() {
        return !usado && !estaExpirado();
    }
    
    /**
     * Marca el token como usado
     */
    public void marcarComoUsado() {
        this.usado = true;
        this.usedAt = LocalDateTime.now();
    }
}