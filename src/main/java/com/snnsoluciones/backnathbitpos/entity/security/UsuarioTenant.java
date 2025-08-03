package com.snnsoluciones.backnathbitpos.entity.security;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_tenant", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "tenant_id"}))
@Data
public class UsuarioTenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "tenant_nombre")
    private String tenantNombre; // Ej: "Mi Restaurante", "Mi Facturación"
    
    @Column(name = "tenant_tipo")
    private String tenantTipo; // Ej: "RESTAURANTE", "FACTURACION"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol; // El rol puede ser diferente en cada tenant
    
    @Column(name = "es_propietario")
    private boolean esPropietario = false;
    
    @Column(name = "activo")
    private boolean activo = true;
    
    @Column(name = "fecha_acceso")
    private LocalDateTime fechaAcceso;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}