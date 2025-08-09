package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa la relación entre usuarios y empresas/sucursales
 * Sin permisos granulares - todo se maneja por rol
 */
@Entity
@Table(name = "usuarios_empresas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_empresa_sucursal",
            columnNames = {"usuario_id", "empresa_id", "sucursal_id"})
    },
    indexes = {
        @Index(name = "idx_usuarios_empresas_usuario", columnList = "usuario_id"),
        @Index(name = "idx_usuarios_empresas_empresa", columnList = "empresa_id"),
        @Index(name = "idx_usuarios_empresas_sucursal", columnList = "sucursal_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "empresa", "sucursal"})
public class UsuarioEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal; // null = acceso a todas las sucursales de la empresa

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(name = "asignado_por")
    private Long asignadoPor;

    @Column(name = "revocado_por")
    private Long revocadoPor;

    @Column(length = 500)
    private String notas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Métodos de utilidad

    /**
     * Verifica si el usuario tiene acceso a todas las sucursales
     */
    public boolean tieneAccesoTodasSucursales() {
        return sucursal == null;
    }

    /**
     * Verifica si la asignación está activa y vigente
     */
    public boolean esAsignacionVigente() {
        return activo && fechaRevocacion == null;
    }

    /**
     * Revoca el acceso
     */
    public void revocarAcceso(Long revocadoPorId) {
        this.activo = false;
        this.fechaRevocacion = LocalDateTime.now();
        this.revocadoPor = revocadoPorId;
    }

    /**
     * Reactiva el acceso
     */
    public void reactivarAcceso() {
        this.activo = true;
        this.fechaRevocacion = null;
        this.revocadoPor = null;
    }
}