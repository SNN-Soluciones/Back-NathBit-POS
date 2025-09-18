package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero_identificacion", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_proveedor_empresa", columnList = "empresa_id"),
        @Index(name = "idx_proveedor_identificacion", columnList = "numero_identificacion")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = true)
    private Sucursal sucursal;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", nullable = false)
    private TipoIdentificacion tipoIdentificacion;
    
    @Column(name = "numero_identificacion", nullable = false, length = 20)
    private String numeroIdentificacion;
    
    @Column(name = "nombre_comercial", nullable = false, length = 200)
    private String nombreComercial;
    
    @Column(name = "razon_social", length = 200)
    private String razonSocial;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 100)
    private String email;
    
    @Column(columnDefinition = "TEXT")
    private String direccion;
    
    // Campos adicionales útiles
    @Column(name = "dias_credito")
    @Builder.Default
    private Integer diasCredito = 0;
    
    @Column(name = "contacto_nombre", length = 100)
    private String contactoNombre;
    
    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;
    
    @Column(columnDefinition = "TEXT")
    private String notas;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}