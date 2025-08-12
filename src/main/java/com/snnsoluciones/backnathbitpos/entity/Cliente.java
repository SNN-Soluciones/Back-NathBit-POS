package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clientes", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cliente_sucursal_identificacion_emails",
            columnNames = {"sucursal_id", "numero_identificacion", "emails"}
        )
    },
    indexes = {
        @Index(name = "idx_cliente_sucursal_numero", columnList = "sucursal_id, numero_identificacion"),
        @Index(name = "idx_cliente_sucursal_nombre", columnList = "sucursal_id, razon_social")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", nullable = false, length = 2)
    private TipoIdentificacion tipoIdentificacion;
    
    @Column(name = "numero_identificacion", nullable = false, length = 20)
    private String numeroIdentificacion;
    
    @Column(name = "razon_social", nullable = false, length = 100)
    private String razonSocial;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String emails; // Separados por coma, máximo 4
    
    @Column(name = "telefono_codigo_pais", length = 3)
    private String telefonoCodigoPais;
    
    @Column(name = "telefono_numero", length = 20)
    private String telefonoNumero;
    
    @Column(name = "permite_credito", nullable = false)
    @Builder.Default
    private Boolean permiteCredito = false;
    
    @Column(name = "tiene_exoneracion", nullable = false)
    @Builder.Default
    private Boolean tieneExoneracion = false;
    
    @Column(columnDefinition = "TEXT")
    private String observaciones;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relaciones
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ClienteUbicacion ubicacion;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ClienteExoneracion> exoneraciones = new HashSet<>();
}