package com.snnsoluciones.backnathbitpos.entity.operacion;

import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asignacion_cajas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionCajas {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;
    
    @Column(name = "usuario_nombre", nullable = false)
    private String usuarioNombre;
    
    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;
    
    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @ElementCollection
    @CollectionTable(name = "asignacion_cajas_detalle",
                      joinColumns = @JoinColumn(name = "asignacion_id"))
    @Column(name = "caja_id")
    private List<UUID> cajasIds;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolNombre rol;
    
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;
    
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;
    
    @Column(name = "asignado_por", nullable = false)
    private UUID asignadoPor;
    
    @Column(nullable = false)
    private Boolean activo;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}