package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad LIMPIA para líneas de detalle de factura
 * Solo almacena datos, NO hace cálculos
 * Arquitectura La Jachuda 🚀
 */
@Data
@Entity
@Table(name = "factura_detalles")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"factura", "producto"})
public class FacturaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "unidad_medida", length = 30, nullable = false)
    @Builder.Default
    private String unidadMedida = "Unid";

    // Campos para facturación electrónica
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;

    @Column(name = "detalle", length = 200)
    private String detalle;

    @Column(name = "descripcion_personalizada", length = 150)
    private String descripcionPersonalizada;

    @Column(name = "es_servicio", nullable = false)
    @Builder.Default
    private Boolean esServicio = Boolean.FALSE;

    @Column(name = "aplica_impuesto_servicio", nullable = false)
    @Builder.Default
    private Boolean aplicaImpuestoServicio = Boolean.FALSE;

    @Column(name = "cantidad", precision = 19, scale = 5, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 19, scale = 5, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "monto_total", precision = 19, scale = 5, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "monto_descuento", precision = 19, scale = 5, nullable = false)
    @Builder.Default
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 19, scale = 5, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "monto_impuesto", precision = 19, scale = 5, nullable = false)
    private BigDecimal montoImpuesto;

    @Column(name = "monto_total_linea", precision = 19, scale = 5, nullable = false)
    private BigDecimal montoTotalLinea;

    // Relaciones
    @OneToMany(mappedBy = "facturaDetalle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<FacturaDescuento> descuentos = new ArrayList<>();

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("codigoImpuesto ASC")
    @Builder.Default
    private List<FacturaDetalleImpuesto> impuestos = new ArrayList<>();

    @Transient
    private List<Long> opcionesSeleccionadas;

    // Métodos helper solo para agregar relaciones

    public void agregarDescuento(FacturaDescuento descuento) {
        if (descuentos.size() >= 5) {
            throw new IllegalStateException("Máximo 5 descuentos permitidos por línea");
        }
        descuentos.add(descuento);
        descuento.setFacturaDetalle(this);
    }

    public void agregarImpuesto(FacturaDetalleImpuesto impuesto) {
        impuestos.add(impuesto);
        impuesto.setDetalle(this);
    }
}