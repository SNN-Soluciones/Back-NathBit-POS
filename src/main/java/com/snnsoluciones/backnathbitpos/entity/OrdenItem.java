package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cantidad = BigDecimal.ONE;

    // Precio al momento de la venta (puede cambiar en el futuro)
    @Column(name = "precio_unitario", precision = 18, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    // Descuento aplicado
    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "monto_descuento", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    // Impuesto
    @Column(name = "tarifa_impuesto", precision = 5, scale = 2)
    private BigDecimal tarifaImpuesto;

    @Column(name = "monto_impuesto", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montoImpuesto = BigDecimal.ZERO;

    // Totales
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "total_descuento", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(name = "total_impuesto", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal total;

    // Para productos compuestos
    @OneToMany(mappedBy = "ordenItemPadre", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrdenItemOpcion> opciones = new ArrayList<>();

    // Notas especiales del item
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Estado del item para cocina
    @Column(name = "enviado_cocina")
    @Builder.Default
    private Boolean enviadoCocina = false;

    @Column(name = "fecha_envio_cocina")
    private LocalDateTime fechaEnvioCocina;

    @Column(name = "preparado")
    @Builder.Default
    private Boolean preparado = false;

    @Column(name = "fecha_preparado")
    private LocalDateTime fechaPreparado;

    @Column(name = "entregado")
    @Builder.Default
    private Boolean entregado = false;

    @Column(name = "fecha_entregado")
    private LocalDateTime fechaEntregado;

    // Para splits de cuenta
    @Column(name = "split_personas")
    private String splitPersonas; // "1,3" significa personas 1 y 3 comparten este item

    // Métodos de cálculo
    public void calcularTotales() {
        // Subtotal base
        this.subtotal = precioUnitario.multiply(cantidad);

        // Calcular descuento
        if (porcentajeDescuento != null && porcentajeDescuento.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDescuento = subtotal.multiply(porcentajeDescuento).divide(new BigDecimal("100"));
        } else if (montoDescuento != null && montoDescuento.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDescuento = montoDescuento.multiply(cantidad);
        } else {
            this.totalDescuento = BigDecimal.ZERO;
        }

        // Subtotal después de descuento
        BigDecimal subtotalConDescuento = subtotal.subtract(totalDescuento);

        // Calcular impuesto sobre el monto con descuento
        if (tarifaImpuesto != null && tarifaImpuesto.compareTo(BigDecimal.ZERO) > 0) {
            this.totalImpuesto = subtotalConDescuento.multiply(tarifaImpuesto).divide(new BigDecimal("100"));
        } else {
            this.totalImpuesto = BigDecimal.ZERO;
        }

        // Total final
        this.total = subtotalConDescuento.add(totalImpuesto);
    }

    public void agregarOpcion(OrdenItemOpcion opcion) {
        opciones.add(opcion);
        opcion.setOrdenItemPadre(this);
    }

    public void marcarEnviadoCocina() {
        this.enviadoCocina = true;
        this.fechaEnvioCocina = LocalDateTime.now();
    }

    public void marcarPreparado() {
        this.preparado = true;
        this.fechaPreparado = LocalDateTime.now();
    }

    public void marcarEntregado() {
        this.entregado = true;
        this.fechaEntregado = LocalDateTime.now();
    }
}