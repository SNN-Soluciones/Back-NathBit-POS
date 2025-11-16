package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import jakarta.persistence.*;
import java.math.RoundingMode;
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

    @Column(nullable = false, unique = false)
    private String numero;

    @ManyToOne
    @JoinColumn(name = "mesa_id")
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
    @Builder.Default
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

        // Calcular impuestos (ya incluye servicio de cada item)
        this.totalImpuesto = items.stream()
            .map(OrdenItem::getTotalImpuesto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🎯 Calcular servicio SOLO para el campo informativo
        // (NO se suma al total porque ya está en totalImpuesto)
        BigDecimal totalServicio = BigDecimal.ZERO;
        for (OrdenItem item : items) {
            if (item.getProducto() != null && Boolean.TRUE.equals(item.getProducto().getEsServicio())) {
                BigDecimal baseImponibleItem = item.getSubtotal().subtract(item.getTotalDescuento());
                BigDecimal servicioItem = baseImponibleItem
                    .multiply(new BigDecimal("10"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalServicio = totalServicio.add(servicioItem);
            }
        }
        this.totalServicio = totalServicio;

        // Total final = subtotal - descuentos + impuestos
        // (el servicio YA está incluido en totalImpuesto)
        this.total = subtotal
            .subtract(totalDescuento)
            .add(totalImpuesto);
    }

    public boolean esOrdenVentanilla() {
        return this.mesa == null;
    }

    public String getIdentificador() {
        if (mesa != null) {
            return mesa.getCodigo(); // "M-01"
        }
        return "VENTANILLA-" + this.numero.substring(this.numero.length() - 5); // "VENTANILLA-00123"
    }

    public boolean puedeModificarse() {
        return estado == EstadoOrden.ABIERTA;
    }

    public boolean puedeFacturarse() {
        return estado == EstadoOrden.ABIERTA && !items.isEmpty();
    }

    /**
     * Calcula todos los totales de la orden basándose en sus items
     */
    public void calcularTotales() {
        // Inicializar totales
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;
        BigDecimal totalServicio = BigDecimal.ZERO;

        // Calcular desde los items
        for (OrdenItem item : this.items) {
            // Asegurarse que el item tiene sus totales calculados
            item.calcularTotales();

            subtotal = subtotal.add(item.getSubtotal());
            totalDescuento = totalDescuento.add(item.getTotalDescuento());
            totalImpuesto = totalImpuesto.add(item.getTotalImpuesto());

            // 🎯 Calcular el servicio SOLO de items que son servicios (para el campo total_servicio)
            if (item.getProducto() != null && Boolean.TRUE.equals(item.getProducto().getEsServicio())) {
                BigDecimal baseImponibleItem = item.getSubtotal().subtract(item.getTotalDescuento());
                BigDecimal servicioItem = baseImponibleItem
                    .multiply(new BigDecimal("10"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                totalServicio = totalServicio.add(servicioItem);
            }
        }

        // Total final = subtotal - descuentos + impuestos
        // (el servicio ya está incluido en totalImpuesto de cada item)
        BigDecimal total = subtotal
            .subtract(totalDescuento)
            .add(totalImpuesto);

        // Asignar valores calculados
        this.subtotal = subtotal;
        this.totalDescuento = totalDescuento;
        this.totalImpuesto = totalImpuesto;
        this.totalServicio = totalServicio; // Solo para reporting, no afecta el total
        this.total = total;
    }
}