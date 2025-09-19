package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sucursales",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"nombre", "empresa_id"}),
        @UniqueConstraint(columnNames = {"codigo_sucursal", "empresa_id"})
    },
    indexes = {
        @Index(name = "idx_sucursal_empresa", columnList = "empresa_id"),
        @Index(name = "idx_sucursal_codigo", columnList = "codigo_sucursal")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "codigo_sucursal", nullable = false, length = 10)
    private String codigoSucursal;

    @Column(length = 200)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean esMatriz = false;

    @Column(name = "modo_facturacion", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModoFacturacion modoFacturacion = ModoFacturacion.ELECTRONICO;

    // NUEVOS CAMPOS PARA FASE 2

    /**
     * Define si esta sucursal maneja control de inventario
     * Si es false, no se valida stock al vender
     */
    @Column(name = "maneja_inventario", nullable = false)
    @Builder.Default
    private Boolean manejaInventario = true;

    /**
     * Define si esta sucursal permite productos con recetas
     * Si es false, solo puede vender productos de inventario SIMPLE o NINGUNO
     */
    @Column(name = "aplica_recetas", nullable = false)
    @Builder.Default
    private Boolean aplicaRecetas = true;

    /**
     * Define si permite inventario negativo (vender sin stock)
     * Solo aplica si manejaInventario = true
     */
    @Column(name = "permite_negativos", nullable = false)
    @Builder.Default
    private Boolean permiteNegativos = false;

    // Campos existentes de auditoría
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Métodos helper
    public boolean puedeVenderProductoConReceta() {
        return this.aplicaRecetas;
    }

    public boolean requiereValidarStock() {
        return this.manejaInventario && !this.permiteNegativos;
    }

    public boolean puedeVenderSinStock() {
        return !this.manejaInventario || this.permiteNegativos;
    }
}