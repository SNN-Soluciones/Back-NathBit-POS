package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero; // ORD-2024-00001

    @ManyToOne(optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mesero_id", nullable = false)
    private Usuario mesero; // Usuario que atiende

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoOrden estado = EstadoOrden.ABIERTA;

    // Cliente opcional (para delivery o llevar)
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "nombre_cliente")
    private String nombreCliente; // Para órdenes rápidas sin cliente registrado

    @Column(name = "numero_personas")
    @Builder.Default
    private Integer numeroPersonas = 1;

    // Items de la orden
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrdenItem> items = new ArrayList<>();

    // Totales
    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalServicio = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    // Porcentaje de servicio (10% default en CR)
    @Column(name = "porcentaje_servicio", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeServicio = new BigDecimal("10.00");

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Relación con factura (cuando se cierre la orden)
    @OneToOne
    @JoinColumn(name = "factura_id")
    private Factura factura;

    // Para manejar splits de cuenta
    @Column(name = "es_split")
    @Builder.Default
    private Boolean esSplit = false;

    @ManyToOne
    @JoinColumn(name = "orden_padre_id")
    private Orden ordenPadre; // Si es un split, referencia a la orden original

    @OneToMany(mappedBy = "ordenPadre")
    private List<Orden> splits = new ArrayList<>();

    // Timestamps
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // Métodos de utilidad
    public void agregarItem(OrdenItem item) {
        items.add(item);
        item.setOrden(this);
        recalcularTotales();
    }

    public void removerItem(OrdenItem item) {
        items.remove(item);
        item.setOrden(null);
        recalcularTotales();
    }

    public void recalcularTotales() {
        // Calcular subtotal
        this.subtotal = items.stream()
            .map(OrdenItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular descuentos
        this.totalDescuento = items.stream()
            .map(OrdenItem::getTotalDescuento)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular impuestos
        this.totalImpuesto = items.stream()
            .map(OrdenItem::getTotalImpuesto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular servicio
        if (porcentajeServicio != null && porcentajeServicio.compareTo(BigDecimal.ZERO) > 0) {
            this.totalServicio = subtotal.multiply(porcentajeServicio).divide(new BigDecimal("100"));
        } else {
            this.totalServicio = BigDecimal.ZERO;
        }

        // Total final
        this.total = subtotal
            .subtract(totalDescuento)
            .add(totalImpuesto)
            .add(totalServicio);
    }

    public boolean puedeModificarse() {
        return estado == EstadoOrden.ABIERTA;
    }

    public boolean puedeFacturarse() {
        return estado == EstadoOrden.ABIERTA && !items.isEmpty();
    }
}