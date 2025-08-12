package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "producto_impuestos",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"producto_id", "tipo_impuesto"})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_impuesto", nullable = false)  // ✅ CORREGIDO: @Column, no @JoinColumn
    private TipoImpuesto tipoImpuesto = TipoImpuesto.IVA;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_tarifa_iva")  // ✅ CORREGIDO: @Column, no @JoinColumn
    private CodigoTarifaIVA tarifaIva = CodigoTarifaIVA.TARIFA_GENERAL_13;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Transient
    public BigDecimal getPorcentajeEfectivo() {
        if (tipoImpuesto == TipoImpuesto.IVA && tarifaIva != null) {
            return tarifaIva.getPorcentaje();
        }
        return porcentaje;
    }
}