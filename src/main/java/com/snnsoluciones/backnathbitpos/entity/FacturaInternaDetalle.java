package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "factura_interna_detalles")
@Data
public class FacturaInternaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaInterna factura;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "codigo_producto", nullable = false)
    private String codigoProducto;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida")
    private String unidadMedida = "Unid";

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "monto_descuento", precision = 15, scale = 2)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal; // (cantidad * precio) - descuento

    @Column(name = "monto_impuesto_servicio", precision = 15, scale = 2)
    private BigDecimal montoImpuestoServicio = BigDecimal.ZERO;

    @Column(name = "monto_total_linea", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotalLinea; // subtotal + impuesto servicio

    @Column(name = "notas")
    private String notas;
}