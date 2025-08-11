package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "producto_impuestos",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"producto_id", "tipo_impuesto_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImpuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_impuesto_id", nullable = false)
    private TipoImpuesto tipoImpuesto;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_iva_id")
    private TarifaIVA tarifaIva; // Solo si es IVA
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}