package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa el registro de asistencia de un usuario.
 * 
 * Cada usuario puede tener una sola asistencia por día (constraint unique).
 * La asistencia tiene hora_entrada y hora_salida.
 * Si hora_salida es NULL, significa que el usuario tiene entrada activa.
 */
@Entity
@Table(name = "asistencias",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_asistencia_usuario_fecha", 
                         columnNames = {"usuario_id", "fecha"})
    },
    indexes = {
        @Index(name = "idx_asistencias_usuario_fecha", 
               columnList = "usuario_id, fecha DESC"),
        @Index(name = "idx_asistencias_fecha", 
               columnList = "fecha DESC"),
        @Index(name = "idx_asistencias_empresa", 
               columnList = "empresa_id"),
        @Index(name = "idx_asistencias_sucursal", 
               columnList = "sucursal_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asistencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Usuario que marca la asistencia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    /**
     * Empresa en la que marca
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    /**
     * Sucursal en la que marca
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    /**
     * Fecha de la asistencia (solo fecha, sin hora)
     */
    @Column(nullable = false)
    private LocalDate fecha;
    
    /**
     * Hora completa de entrada (timestamp)
     */
    @Column(name = "hora_entrada")
    private LocalDateTime horaEntrada;
    
    /**
     * Hora completa de salida (timestamp)
     * NULL = entrada activa (no ha salido)
     */
    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;
    
    /**
     * Observaciones adicionales (opcional)
     */
    @Column(columnDefinition = "TEXT")
    private String observaciones;
    
    /**
     * Fecha de creación del registro
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Fecha de última actualización
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Verifica si la entrada está activa (no ha salido)
     */
    public boolean tieneEntradaActiva() {
        return horaSalida == null;
    }
    
    /**
     * Marca la salida con la hora actual
     */
    public void marcarSalida() {
        this.horaSalida = LocalDateTime.now();
    }
    
    /**
     * Marca la entrada con la hora actual
     */
    public void marcarEntrada() {
        this.horaEntrada = LocalDateTime.now();
        this.horaSalida = null; // Asegurar que salida esté en null
    }
}