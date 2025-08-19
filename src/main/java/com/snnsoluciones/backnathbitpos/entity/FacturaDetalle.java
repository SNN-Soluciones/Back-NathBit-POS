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

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", length = 15, nullable = false)
    private String unidadMedida = "Unid";

    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioUnitario;

    // Campos para facturación electrónica
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;

    @Column(name = "detalle", length = 200)
    private String detalle;

    /**
     * Indica si es servicio (true) o mercancía (false)
     * Necesario para los totales del ResumenFactura
     */
    @Column(name = "es_servicio", nullable = false)
    private Boolean esServicio = false;

    /**
     * Indica si esta línea aplica el impuesto de servicio 10%
     * Solo marca, el cálculo lo hace el frontend
     */
    @Column(name = "aplica_impuesto_servicio", nullable = false)
    private Boolean aplicaImpuestoServicio = false;

    // Montos calculados por el frontend
    @Column(name = "monto_total", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoTotal;

    @Column(name = "monto_descuento", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal subtotal;

    @Column(name = "monto_impuesto", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoImpuesto = BigDecimal.ZERO;

    @Column(name = "monto_total_linea", nullable = false, precision = 18, scale = 5)
    private BigDecimal montoTotalLinea;

    // Relaciones
    @OneToMany(mappedBy = "facturaDetalle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<FacturaDescuento> descuentos = new ArrayList<>();

    @OneToMany(mappedBy = "facturaDetalle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("codigoImpuesto ASC")
    private List<FacturaDetalleImpuesto> impuestos = new ArrayList<>();

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
        impuesto.setFacturaDetalle(this);
    }
}