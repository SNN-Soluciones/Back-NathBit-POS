package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un dispositivo PDV registrado en el sistema.
 * 
 * Cada dispositivo tiene un token permanente (device_token) que se almacena
 * en el PDV (en Capacitor Preferences) y se usa para autenticación.
 * 
 * El dispositivo está asociado a una empresa y sucursal específica.
 */
@Entity
@Table(name = "dispositivos",
    indexes = {
        @Index(name = "idx_dispositivos_device_token", columnList = "device_token"),
        @Index(name = "idx_dispositivos_empresa", columnList = "empresa_id"),
        @Index(name = "idx_dispositivos_sucursal", columnList = "sucursal_id"),
        @Index(name = "idx_dispositivos_activo", columnList = "activo")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispositivo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Token permanente del dispositivo (formato: DEV-xxxxx)
     * Este token se guarda en el PDV y se usa para todas las peticiones
     */
    @Column(name = "device_token", nullable = false, unique = true, length = 255)
    private String deviceToken;
    
    /**
     * Nombre descriptivo del dispositivo
     * Ejemplos: "Tablet Caja 1", "iPad Bar", "Samsung Cocina"
     */
    @Column(nullable = false, length = 255)
    private String nombre;
    
    /**
     * Empresa a la que pertenece el dispositivo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    /**
     * Sucursal donde opera el dispositivo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    /**
     * UUID único del hardware del dispositivo
     * Se obtiene del dispositivo físico (Android/iOS)
     */
    @Column(name = "uuid_hardware", length = 255)
    private String uuidHardware;
    
    /**
     * Modelo del dispositivo
     * Ejemplo: "Samsung Galaxy Tab A8", "iPad Air 2022"
     */
    @Column(length = 255)
    private String modelo;
    
    /**
     * Plataforma del dispositivo
     * Valores: "ANDROID", "IOS", "WEB"
     */
    @Column(length = 50)
    private String plataforma;
    
    /**
     * User agent del navegador/app
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    /**
     * IP desde donde se registró el dispositivo
     */
    @Column(name = "ip_registro", length = 255)
    private String ipRegistro;
    
    /**
     * Indica si el dispositivo está activo
     * Un admin puede desactivar un dispositivo para bloquear su acceso
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    /**
     * Última vez que el dispositivo hizo una petición al backend
     */
    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;
    
    /**
     * Fecha y hora de registro del dispositivo
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Fecha y hora de última actualización
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Nombre de la sucursal (desnormalizado para facilitar consultas)
     */
    @Column(name = "sucursal_nombre", length = 255)
    private String sucursalNombre;
    
    /**
     * Actualiza el timestamp de último uso
     */
    public void registrarUso() {
        this.ultimoUso = LocalDateTime.now();
    }
    
    /**
     * Activa el dispositivo
     */
    public void activar() {
        this.activo = true;
    }
    
    /**
     * Desactiva el dispositivo (bloquea su acceso)
     */
    public void desactivar() {
        this.activo = false;
    }
    
    /**
     * Verifica si el dispositivo está activo
     */
    public boolean estaActivo() {
        return activo != null && activo;
    }
}