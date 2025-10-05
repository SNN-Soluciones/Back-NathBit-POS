package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.DetalleFacturaInternaRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Detalle de líneas de factura interna - SÚPER SIMPLE
 */
@Entity
@Table(name = "factura_interna_detalle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class FacturaInternaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_interna_id", nullable = false)
    private FacturaInterna facturaInterna;

    // ===== PRODUCTO =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "codigo_producto", length = 50)
    private String codigoProducto;

    @Column(name = "nombre_producto", nullable = false, length = 255)
    private String nombreProducto;

    // ===== CANTIDADES Y PRECIOS =====
    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal; // cantidad * precioUnitario

    @Column(name = "descuento", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total; // subtotal - descuento

    // ===== NOTAS =====
    @Column(name = "notas", length = 500)
    private String notas;

    // ===== MÉTODOS HELPER =====

    /**
     * Calcula los totales de la línea
     */
    public void calcularTotales() {
        // Subtotal = cantidad * precio unitario
        this.subtotal = this.cantidad.multiply(this.precioUnitario);

        // Total = subtotal - descuento
        this.total = this.subtotal.subtract(
            this.descuento != null ? this.descuento : BigDecimal.ZERO
        );
    }

    /**
     * Setea la información del producto
     */
    public void setearDatosProducto(Producto producto, DetalleFacturaInternaRequest detalleFacturaInternaRequest) {
        this.producto = producto;
        this.codigoProducto = producto.getCodigoInterno();
        this.nombreProducto = producto.getNombre();
        this.precioUnitario = detalleFacturaInternaRequest.getPrecioUnitario();
    }
}