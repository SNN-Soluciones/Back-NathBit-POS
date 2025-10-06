package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Detalle de líneas de productos/servicios de una factura recibida
 * Estructura ESPEJO de FacturaDetalle
 */
@Entity
@Table(name = "facturas_recepcion_detalles",
    indexes = {
        @Index(name = "idx_factura_recepcion_detalle_factura", columnList = "factura_recepcion_id"),
        @Index(name = "idx_factura_recepcion_detalle_cabys", columnList = "codigo_cabys")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"facturaRecepcion", "impuestos", "descuentos"})
@ToString(exclude = {"facturaRecepcion", "impuestos", "descuentos"})
public class FacturaRecepcionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_recepcion_id", nullable = false)
    private FacturaRecepcion facturaRecepcion;

    @OneToMany(mappedBy = "facturaRecepcionDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<FacturaRecepcionDescuento> descuentos = new ArrayList<>();

    @OneToMany(mappedBy = "facturaRecepcionDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaRecepcionDetalleImpuesto> impuestos = new ArrayList<>();

    // ==================== IDENTIFICACIÓN DEL ITEM ====================

    /**
     * Número de línea (1, 2, 3...)
     */
    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    /**
     * Código CABYS (13 dígitos)
     */
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;

    private String tipoCodigoComercial;

    /**
     * Código comercial del proveedor
     */
    @Column(name = "codigo_comercial", length = 50)
    private String codigoComercial;

    /**
     * Descripción del producto/servicio
     */
    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    /**
     * Detalle adicional
     */
    @Column(name = "detalle", length = 200)
    private String detalle;

    /**
     * Es un servicio (true) o mercancía (false)
     */
    @Column(name = "es_servicio", nullable = false)
    @Builder.Default
    private Boolean esServicio = false;

    // ==================== CANTIDADES Y UNIDADES ====================

    @Column(name = "cantidad", precision = 18, scale = 5, nullable = false)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", length = 20, nullable = false)
    @Builder.Default
    private UnidadMedida unidadMedida = UnidadMedida.UNIDAD;

    @Column(name = "unidad_medida_comercial", length = 50)
    private String unidadMedidaComercial;

    // ==================== PRECIOS Y MONTOS ====================

    @Column(name = "precio_unitario", precision = 18, scale = 5, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "monto_total", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "monto_descuento", precision = 18, scale = 5)
    @Builder.Default
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 18, scale = 5, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "base_imponible", precision = 18, scale = 5)
    private BigDecimal baseImponible;

    @Column(name = "monto_impuesto", precision = 18, scale = 5, nullable = false)
    @Builder.Default
    private BigDecimal montoImpuesto = BigDecimal.ZERO;

    @Column(name = "monto_total_linea", precision = 18, scale = 5, nullable = false)
    private BigDecimal montoTotalLinea;

    // ==================== HELPER METHODS ====================

    public void addDescuento(FacturaRecepcionDescuento descuento) {
        if (descuentos.size() >= 5) {
            throw new IllegalStateException("Máximo 5 descuentos permitidos por línea según Hacienda");
        }
        descuentos.add(descuento);
        descuento.setFacturaRecepcionDetalle(this);
    }

    public void removeDescuento(FacturaRecepcionDescuento descuento) {
        descuentos.remove(descuento);
        descuento.setFacturaRecepcionDetalle(null);
    }

    public void addImpuesto(FacturaRecepcionDetalleImpuesto impuesto) {
        impuestos.add(impuesto);
        impuesto.setFacturaRecepcionDetalle(this);
    }

    public void removeImpuesto(FacturaRecepcionDetalleImpuesto impuesto) {
        impuestos.remove(impuesto);
        impuesto.setFacturaRecepcionDetalle(null);
    }
}